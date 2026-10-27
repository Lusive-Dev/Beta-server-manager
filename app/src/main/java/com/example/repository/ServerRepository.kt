package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.engine.LanBroadcastManager
import com.example.engine.ProcessShellRunner
import com.example.engine.ServerFileManager
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.ServerConfig
import com.example.model.ServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ServerRepository private constructor(private val context: Context) {

    val fileManager = ServerFileManager(context)
    val broadcastManager = LanBroadcastManager(context)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _config = MutableStateFlow(fileManager.loadServerConfig())
    val config: StateFlow<ServerConfig> = _config.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(
                rawText = "[SYSTEM] MC Beta Host Linux Shell initialized in ${fileManager.serverDir.name}/",
                level = LogLevel.SYSTEM
            ),
            LogEntry(
                rawText = "[SYSTEM] Ready. Type any shell command or start minecraft_server.jar.",
                level = LogLevel.SYSTEM
            )
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _localIp = MutableStateFlow(broadcastManager.getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _connectedPlayers = MutableStateFlow<List<String>>(emptyList())
    val connectedPlayers: StateFlow<List<String>> = _connectedPlayers.asStateFlow()

    val shellRunner = ProcessShellRunner(
        context = context,
        fileManager = fileManager,
        onLog = { entry -> appendLog(entry) },
        onServerTerminated = { code ->
            _serverState.value = ServerState.Stopped
            broadcastManager.stopBroadcasting()
        }
    )

    fun refreshNetworkIp() {
        _localIp.value = broadcastManager.getLocalIpAddress()
    }

    fun updateConfig(newConfig: ServerConfig) {
        _config.value = newConfig
        fileManager.saveServerConfig(newConfig)
        appendLog(LogEntry.parse("[SYSTEM] Server settings saved (Cracked: ${!newConfig.onlineMode}, Port: ${newConfig.serverPort})"))
    }

    fun toggleCrackedMode(isCracked: Boolean) {
        val updated = _config.value.copy(onlineMode = !isCracked)
        updateConfig(updated)
    }

    fun toggleAutoEula(autoEula: Boolean) {
        val updated = _config.value.copy(autoEula = autoEula)
        if (autoEula) {
            fileManager.ensureEulaAgreed(true)
        }
        updateConfig(updated)
    }

    fun appendLog(entry: LogEntry) {
        _logs.update { list ->
            if (list.size > 800) {
                list.drop(100) + entry
            } else {
                list + entry
            }
        }
    }

    fun clearLogs() {
        _logs.value = listOf(
            LogEntry(rawText = "[SYSTEM] Console logs cleared.", level = LogLevel.SYSTEM)
        )
    }

    fun sendCommand(command: String) {
        repositoryScope.launch {
            shellRunner.executeCommand(command)
        }
    }

    fun startServer() {
        val currentState = _serverState.value
        if (currentState is ServerState.Running || currentState is ServerState.Starting) return

        repositoryScope.launch {
            _serverState.value = ServerState.Starting("Checking environment...")
            refreshNetworkIp()

            val currentCfg = _config.value
            val result = shellRunner.startServer(this, currentCfg)

            when (result) {
                is ProcessShellRunner.StartResult.Success -> {
                    val ip = _localIp.value
                    val port = currentCfg.serverPort
                    val isCracked = !currentCfg.onlineMode

                    _serverState.value = ServerState.Running(
                        startTimeMs = System.currentTimeMillis(),
                        port = port,
                        ipAddress = ip,
                        processPid = result.pid,
                        isCracked = isCracked
                    )

                    if (currentCfg.autoLanBroadcast) {
                        broadcastManager.startBroadcasting(this, currentCfg.motd, port)
                    }
                }
                is ProcessShellRunner.StartResult.MissingJar -> {
                    _serverState.value = ServerState.Error("minecraft_server.jar is missing. Please download or import the JAR first.")
                }
                is ProcessShellRunner.StartResult.LaunchFailed -> {
                    _serverState.value = ServerState.Error("Process launch failed: ${result.message}")
                }
                is ProcessShellRunner.StartResult.AlreadyRunning -> {}
            }
        }
    }

    fun stopServer() {
        val currentState = _serverState.value
        if (currentState !is ServerState.Running && currentState !is ServerState.Starting) return

        _serverState.value = ServerState.Stopping
        shellRunner.stopServer()
        broadcastManager.stopBroadcasting()
        _serverState.value = ServerState.Stopped
    }

    fun downloadServerJar(onComplete: (Boolean) -> Unit) {
        repositoryScope.launch {
            _serverState.value = ServerState.DownloadingJar(0.05f, "Connecting to Minecraft archive...")
            val result = fileManager.downloadOfficialBeta17Jar { progress, message ->
                _serverState.value = ServerState.DownloadingJar(progress, message)
            }
            if (result.isSuccess) {
                appendLog(LogEntry.parse("[SYSTEM] Downloaded official Beta 1.7.3 server.jar successfully!"))
                _serverState.value = ServerState.Stopped
                onComplete(true)
            } else {
                appendLog(LogEntry.parse("[ERROR] Download failed: ${result.exceptionOrNull()?.message}"))
                _serverState.value = ServerState.Error("Download failed: ${result.exceptionOrNull()?.message}")
                onComplete(false)
            }
        }
    }

    fun importCustomJar(uri: Uri, onComplete: (Boolean) -> Unit) {
        repositoryScope.launch {
            _serverState.value = ServerState.DownloadingJar(0.2f, "Importing custom JAR file...")
            val result = fileManager.importCustomJar(uri)
            if (result.isSuccess) {
                appendLog(LogEntry.parse("[SYSTEM] Custom server.jar imported successfully!"))
                _serverState.value = ServerState.Stopped
                onComplete(true)
            } else {
                appendLog(LogEntry.parse("[ERROR] Custom JAR import failed: ${result.exceptionOrNull()?.message}"))
                _serverState.value = ServerState.Error("Import failed: ${result.exceptionOrNull()?.message}")
                onComplete(false)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ServerRepository? = null

        fun getInstance(context: Context): ServerRepository {
            return instance ?: synchronized(this) {
                instance ?: ServerRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
