package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.LogEntry
import com.example.model.ServerConfig
import com.example.model.ServerMetrics
import com.example.model.ServerState
import com.example.repository.ServerRepository
import com.example.service.ServerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServerRepository.getInstance(application)

    val serverState: StateFlow<ServerState> = repository.serverState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ServerState.Stopped)

    val config: StateFlow<ServerConfig> = repository.config
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.config.value)

    val logs: StateFlow<List<LogEntry>> = repository.logs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val localIp: StateFlow<String> = repository.localIp
        .stateIn(viewModelScope, SharingStarted.Eagerly, "127.0.0.1")

    val connectedPlayers: StateFlow<List<String>> = repository.connectedPlayers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _opsList = MutableStateFlow<List<String>>(emptyList())
    val opsList: StateFlow<List<String>> = _opsList.asStateFlow()

    private val _whitelist = MutableStateFlow<List<String>>(emptyList())
    val whitelist: StateFlow<List<String>> = _whitelist.asStateFlow()

    private val _bannedList = MutableStateFlow<List<String>>(emptyList())
    val bannedList: StateFlow<List<String>> = _bannedList.asStateFlow()

    private val _serverMetrics = MutableStateFlow(ServerMetrics())
    val serverMetrics: StateFlow<ServerMetrics> = _serverMetrics.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    init {
        refreshPlayerLists()
        repository.refreshNetworkIp()
        startMetricsCollector()
    }

    private fun startMetricsCollector() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val state = serverState.value
                val cfg = config.value
                val hasJar = repository.fileManager.hasServerJar()
                val jarBytes = if (hasJar) repository.fileManager.serverJarFile.length() else 0L
                val jarSizeFormatted = repository.fileManager.getServerJarSizeFormatted()
                val worldBytes = repository.fileManager.getWorldFolderSizeBytes()
                val worldSizeFormatted = repository.fileManager.getWorldFolderSizeFormatted()

                val runtime = Runtime.getRuntime()
                val realUsedRamMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()

                if (state is ServerState.Running) {
                    val uptimeSec = ((System.currentTimeMillis() - state.startTimeMs) / 1000).coerceAtLeast(0)
                    val hours = uptimeSec / 3600
                    val minutes = (uptimeSec % 3600) / 60
                    val seconds = uptimeSec % 60
                    val uptimeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                    _serverMetrics.value = ServerMetrics(
                        isRunning = true,
                        processPid = state.processPid,
                        uptimeSeconds = uptimeSec,
                        formattedUptime = uptimeFormatted,
                        usedRamMb = realUsedRamMb,
                        maxConfiguredRamMb = cfg.memoryMb,
                        hasServerJar = hasJar,
                        serverJarSizeBytes = jarBytes,
                        serverJarSizeFormatted = jarSizeFormatted,
                        worldFolderSizeBytes = worldBytes,
                        worldFolderSizeFormatted = worldSizeFormatted,
                        serverPort = state.port,
                        lastExitCode = repository.shellRunner.lastExitCode
                    )
                } else {
                    _serverMetrics.value = ServerMetrics(
                        isRunning = false,
                        processPid = null,
                        uptimeSeconds = 0L,
                        formattedUptime = "00:00:00",
                        usedRamMb = realUsedRamMb,
                        maxConfiguredRamMb = cfg.memoryMb,
                        hasServerJar = hasJar,
                        serverJarSizeBytes = jarBytes,
                        serverJarSizeFormatted = jarSizeFormatted,
                        worldFolderSizeBytes = worldBytes,
                        worldFolderSizeFormatted = worldSizeFormatted,
                        serverPort = cfg.serverPort,
                        lastExitCode = repository.shellRunner.lastExitCode
                    )
                }
            }
        }
    }

    fun hasServerJar(): Boolean = repository.fileManager.hasServerJar()
    fun getServerJarSize(): String = repository.fileManager.getServerJarSizeFormatted()
    fun isEulaAgreed(): Boolean = repository.fileManager.isEulaAgreed()

    fun startServer() {
        ServerService.start(getApplication())
    }

    fun stopServer() {
        ServerService.stop(getApplication())
    }

    fun toggleCrackedMode(isCracked: Boolean) {
        repository.toggleCrackedMode(isCracked)
        postFeedback(if (isCracked) "Cracked Mode Enabled: Offline clients can join." else "Online Mode Enabled: Mojang auth required.")
    }

    fun toggleAutoEula(autoEula: Boolean) {
        repository.toggleAutoEula(autoEula)
        postFeedback(if (autoEula) "Auto-EULA Agreer: eula=true enforced" else "Auto-EULA disabled")
    }

    fun updateConfig(newConfig: ServerConfig) {
        repository.updateConfig(newConfig)
        postFeedback("Settings saved to server.properties")
    }

    fun sendCommand(command: String) {
        repository.sendCommand(command)
    }

    fun clearLogs() {
        repository.clearLogs()
    }

    fun downloadOfficialJar() {
        repository.downloadServerJar { success ->
            if (success) {
                postFeedback("Minecraft Beta 1.7.3 server.jar ready!")
            } else {
                postFeedback("Download failed. Check internet connection.")
            }
        }
    }

    fun importCustomJar(uri: Uri) {
        repository.importCustomJar(uri) { success ->
            if (success) {
                postFeedback("Custom server.jar imported successfully!")
            } else {
                postFeedback("Failed to import custom JAR file.")
            }
        }
    }

    fun refreshPlayerLists() {
        viewModelScope.launch {
            _opsList.value = repository.fileManager.getOps()
            _whitelist.value = repository.fileManager.getWhitelist()
            _bannedList.value = repository.fileManager.getBannedPlayers()
        }
    }

    fun addOp(name: String) {
        if (name.isBlank()) return
        repository.fileManager.addOp(name)
        sendCommand("op $name")
        refreshPlayerLists()
        postFeedback("Added $name to server operators.")
    }

    fun removeOp(name: String) {
        repository.fileManager.removeOp(name)
        sendCommand("deop $name")
        refreshPlayerLists()
        postFeedback("Removed $name from operators.")
    }

    fun addToWhitelist(name: String) {
        if (name.isBlank()) return
        repository.fileManager.addToWhitelist(name)
        sendCommand("whitelist add $name")
        refreshPlayerLists()
        postFeedback("Added $name to whitelist.")
    }

    fun removeFromWhitelist(name: String) {
        repository.fileManager.removeFromWhitelist(name)
        sendCommand("whitelist remove $name")
        refreshPlayerLists()
        postFeedback("Removed $name from whitelist.")
    }

    fun banPlayer(name: String) {
        if (name.isBlank()) return
        repository.fileManager.banPlayer(name)
        sendCommand("ban $name")
        refreshPlayerLists()
        postFeedback("Banned $name.")
    }

    fun unbanPlayer(name: String) {
        repository.fileManager.unbanPlayer(name)
        sendCommand("pardon $name")
        refreshPlayerLists()
        postFeedback("Unbanned $name.")
    }

    fun kickPlayer(name: String) {
        sendCommand("kick $name")
        postFeedback("Sent kick signal for $name.")
    }

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }

    private fun postFeedback(message: String) {
        _userFeedbackMessage.value = message
    }
}
