package com.example.engine

import android.content.Context
import android.os.Build
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

class ProcessShellRunner(
    private val context: Context,
    val fileManager: ServerFileManager,
    private val onLog: (LogEntry) -> Unit,
    private val onServerTerminated: (Int) -> Unit
) {
    private var serverProcess: Process? = null
    private var serverWriter: BufferedWriter? = null
    private var logReadJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    var currentPid: Long? = null
        private set

    var lastExitCode: Int? = null
        private set

    /**
     * Determines whether java executable exists in internal sandbox or system PATH
     */
    fun findJavaExecutable(customPath: String = ""): File? {
        if (customPath.isNotBlank()) {
            val custom = File(customPath)
            if (custom.exists() && custom.canExecute()) return custom
        }

        // 1. App-internal bundled JRE in context.filesDir/jre/bin/java
        val internalJre = File(fileManager.jreDir, "bin/java")
        if (internalJre.exists()) {
            if (!internalJre.canExecute()) internalJre.setExecutable(true, false)
            return internalJre
        }

        // 2. Termux / System PATH candidates
        val candidatePaths = listOf(
            "/data/data/com.termux/files/usr/bin/java",
            "/system/bin/java",
            "/system/xbin/java",
            "/apex/com.android.runtime/bin/java"
        )
        for (path in candidatePaths) {
            val f = File(path)
            if (f.exists() && f.canExecute()) {
                return f
            }
        }
        return null
    }

    /**
     * Starts the official Minecraft Beta 1.7.3 server.jar
     * Strictly requires the JAR file to exist.
     */
    suspend fun startServer(scope: CoroutineScope, config: ServerConfig): StartResult = withContext(Dispatchers.IO) {
        if (isRunning.get()) {
            return@withContext StartResult.AlreadyRunning
        }

        // Strict Check 1: minecraft_server.jar must exist
        if (!fileManager.hasServerJar()) {
            onLog(LogEntry.parse("[ERROR] Cannot start server: 'minecraft_server.jar' does not exist in ${fileManager.serverDir.absolutePath}"))
            onLog(LogEntry.parse("[ERROR] Please click 'Auto-Download' to fetch the official Beta 1.7.3 server.jar, or use 'Import JAR' to load your own."))
            return@withContext StartResult.MissingJar
        }

        // Auto-EULA
        if (config.autoEula) {
            fileManager.ensureEulaAgreed(true)
            onLog(LogEntry.parse("[SYSTEM] Verified eula.txt (eula=true)."))
        }

        // Save server.properties
        fileManager.saveServerConfig(config)
        onLog(LogEntry.parse("[SYSTEM] Saved server.properties: port=${config.serverPort}, online-mode=${config.onlineMode}"))

        // Strict Check 2: Check for Java runtime
        val javaExe = findJavaExecutable(config.javaCustomPath)
        val javaBinPath = javaExe?.absolutePath ?: "java"

        onLog(LogEntry.parse("[SHELL] cd ${fileManager.serverDir.absolutePath}"))
        onLog(LogEntry.parse("[SHELL] $javaBinPath -Xms${config.memoryMb}M -Xmx${config.memoryMb}M -jar ${fileManager.serverJarFile.name} nogui"))

        try {
            val cmdList = listOf(
                javaBinPath,
                "-Xms${config.memoryMb}M",
                "-Xmx${config.memoryMb}M",
                "-jar",
                fileManager.serverJarFile.absolutePath,
                "nogui"
            )

            val pb = ProcessBuilder(cmdList)
            pb.directory(fileManager.serverDir)
            pb.redirectErrorStream(true)

            // Inject PATH so any dependencies or native libs can be located
            val env = pb.environment()
            val existingPath = env["PATH"] ?: "/system/bin"
            env["PATH"] = "${fileManager.jreDir.absolutePath}/bin:$existingPath"

            val proc = pb.start()
            serverProcess = proc
            serverWriter = BufferedWriter(OutputStreamWriter(proc.outputStream))
            isRunning.set(true)

            // Read process PID if supported
            currentPid = try {
                val f = proc.javaClass.getDeclaredField("pid")
                f.isAccessible = true
                f.getInt(proc).toLong()
            } catch (e: Throwable) {
                null
            }

            onLog(LogEntry.parse("[SYSTEM] Server process spawned (PID: ${currentPid ?: "active"}). Streaming logs..."))

            logReadJob = scope.launch(Dispatchers.IO) {
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                try {
                    var line: String? = null
                    while (isActive && reader.readLine().also { line = it } != null) {
                        line?.let { raw ->
                            onLog(LogEntry.parse(raw))
                        }
                    }
                } catch (e: Exception) {
                    // stream closed
                }

                val exitCode = try { proc.waitFor() } catch (e: Exception) { -1 }
                lastExitCode = exitCode
                isRunning.set(false)
                currentPid = null
                onLog(LogEntry.parse("[SYSTEM] Server process terminated with exit code $exitCode"))
                onServerTerminated(exitCode)
            }

            StartResult.Success(currentPid)
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unknown error spawning process"
            onLog(LogEntry.parse("[ERROR] Failed to execute '$javaBinPath': $errMsg"))
            if (errMsg.contains("No such file", ignoreCase = true) || errMsg.contains("not found", ignoreCase = true)) {
                onLog(LogEntry.parse("[INFO] The Java binary was not found on this system."))
                onLog(LogEntry.parse("[INFO] You can install OpenJDK via Termux (pkg install openjdk-17) or specify the path in Properties."))
            }
            isRunning.set(false)
            currentPid = null
            StartResult.LaunchFailed(errMsg)
        }
    }

    /**
     * Executes an interactive command:
     * - If the server is running, sends the command to the server's stdin.
     * - If the server is not running, executes the command in the actual Linux shell (/system/bin/sh)
     *   inside the server directory!
     */
    suspend fun executeCommand(command: String): Unit = withContext(Dispatchers.IO) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return@withContext

        onLog(LogEntry(rawText = "> $trimmed", level = LogLevel.COMMAND))

        if (isRunning.get() && serverWriter != null) {
            // Send directly to the running server stdin
            try {
                serverWriter?.write(trimmed)
                serverWriter?.newLine()
                serverWriter?.flush()
            } catch (e: Exception) {
                onLog(LogEntry.parse("[ERROR] Failed writing to server stdin: ${e.message}"))
            }
        } else {
            // Execute real shell command in the server directory
            try {
                val pb = ProcessBuilder("/system/bin/sh", "-c", trimmed)
                pb.directory(fileManager.serverDir)
                pb.redirectErrorStream(true)

                val env = pb.environment()
                val existingPath = env["PATH"] ?: "/system/bin"
                env["PATH"] = "${fileManager.jreDir.absolutePath}/bin:$existingPath"

                val proc = pb.start()
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var line: String? = null
                while (reader.readLine().also { line = it } != null) {
                    line?.let { onLog(LogEntry(rawText = it, level = LogLevel.INFO)) }
                }
                val code = proc.waitFor()
                if (code != 0) {
                    onLog(LogEntry.parse("[SHELL] Exit code: $code"))
                }
            } catch (e: Exception) {
                onLog(LogEntry.parse("[ERROR] Shell execution failed: ${e.message}"))
            }
        }
    }

    fun stopServer() {
        if (!isRunning.compareAndSet(true, false)) return

        onLog(LogEntry.parse("[SYSTEM] Sending 'stop' command to Minecraft server process..."))
        try {
            serverWriter?.write("stop\n")
            serverWriter?.flush()
        } catch (e: Exception) {
            // ignore
        }

        // Wait up to 3 seconds for clean save, then terminate if still running
        try {
            val proc = serverProcess
            if (proc != null) {
                Thread.sleep(1000)
                proc.destroy()
            }
        } catch (e: Exception) {
            // ignore
        }

        serverProcess = null
        serverWriter = null
        logReadJob?.cancel()
        logReadJob = null
        currentPid = null
        onServerTerminated(0)
    }

    fun isRunning(): Boolean = isRunning.get() && (serverProcess?.isAlive ?: false)

    sealed class StartResult {
        data class Success(val pid: Long?) : StartResult()
        object MissingJar : StartResult()
        object AlreadyRunning : StartResult()
        data class LaunchFailed(val message: String) : StartResult()
    }
}
