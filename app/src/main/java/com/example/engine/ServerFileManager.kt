package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.concurrent.TimeUnit

class ServerFileManager(private val context: Context) {

    val serverDir: File by lazy {
        File(context.filesDir, "mc_server").apply { if (!exists()) mkdirs() }
    }

    val serverJarFile: File
        get() = File(serverDir, "minecraft_server.jar")

    val eulaFile: File
        get() = File(serverDir, "eula.txt")

    val propertiesFile: File
        get() = File(serverDir, "server.properties")

    val opsFile: File
        get() = File(serverDir, "ops.txt")

    val whitelistFile: File
        get() = File(serverDir, "white-list.txt")

    val bannedPlayersFile: File
        get() = File(serverDir, "banned-players.txt")

    val jreDir: File by lazy {
        File(context.filesDir, "jre").apply { if (!exists()) mkdirs() }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the official or custom server.jar is present
     */
    fun hasServerJar(): Boolean {
        return serverJarFile.exists() && serverJarFile.length() > 100_000
    }

    fun getServerJarSizeFormatted(): String {
        if (!hasServerJar()) return "Missing"
        val bytes = serverJarFile.length()
        return "%.2f MB".format(bytes / (1024.0 * 1024.0))
    }

    val worldDir: File
        get() = File(serverDir, "world")

    fun getWorldFolderSizeBytes(): Long {
        if (!worldDir.exists()) return 0L
        return worldDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    fun getWorldFolderSizeFormatted(): String {
        val bytes = getWorldFolderSizeBytes()
        if (bytes == 0L) return "0 KB"
        if (bytes < 1024 * 1024) return "%d KB".format(bytes / 1024)
        return "%.2f MB".format(bytes / (1024.0 * 1024.0))
    }

    /**
     * Auto-writes eula.txt with eula=true to prevent server exit on first boot
     */
    fun ensureEulaAgreed(agree: Boolean = true) {
        try {
            val date = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US).format(Date())
            val content = "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).\n" +
                    "#Auto-agreed by MC Beta Host\n" +
                    "#$date\n" +
                    "eula=$agree\n"
            eulaFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isEulaAgreed(): Boolean {
        if (!eulaFile.exists()) return false
        return try {
            eulaFile.readLines().any { it.trim().equals("eula=true", ignoreCase = true) }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads server.properties from disk or creates default
     */
    fun loadServerConfig(): ServerConfig {
        if (!propertiesFile.exists()) {
            val defaultConfig = ServerConfig(onlineMode = false, autoEula = true)
            saveServerConfig(defaultConfig)
            return defaultConfig
        }

        return try {
            val props = Properties()
            propertiesFile.inputStream().use { props.load(it) }
            val map = props.entries.associate { it.key.toString() to it.value.toString() }
            ServerConfig.fromPropertiesMap(map).copy(
                autoEula = isEulaAgreed()
            )
        } catch (e: Exception) {
            ServerConfig(onlineMode = false)
        }
    }

    /**
     * Writes server.properties and updates eula.txt
     */
    fun saveServerConfig(config: ServerConfig) {
        try {
            if (config.autoEula) {
                ensureEulaAgreed(true)
            }
            propertiesFile.writeText(config.toPropertiesFileContent())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Downloads official Minecraft Beta 1.7.3 server jar
     */
    suspend fun downloadOfficialBeta17Jar(
        onProgress: (progress: Float, message: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadUrls = listOf(
            "https://launcher.mojang.com/v1/objects/2bb3a928236d8333c5e0a6d54d58852fc165eb34/server.jar",
            "https://archive.org/download/Minecraft-Beta-1.7.3-Server/minecraft_server.jar",
            "https://files.betacraft.uk/server-archive/beta/b1.7.3.jar"
        )

        var lastError: Throwable? = null

        for (url in downloadUrls) {
            try {
                onProgress(0.1f, "Connecting to archive: ${url.substringAfterLast("/")}...")
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    continue
                }

                val body = response.body ?: continue
                val contentLength = body.contentLength()
                val tempFile = File(serverDir, "minecraft_server.jar.tmp")

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = (totalRead.toFloat() / contentLength).coerceIn(0.1f, 0.95f)
                                onProgress(progress, "Downloading Beta 1.7.3 (${(totalRead / 1024)} KB / ${(contentLength / 1024)} KB)...")
                            }
                        }
                    }
                }

                if (tempFile.exists() && tempFile.length() > 50_000) {
                    if (serverJarFile.exists()) serverJarFile.delete()
                    tempFile.renameTo(serverJarFile)
                    ensureEulaAgreed(true)
                    onProgress(1.0f, "Beta 1.7.3 Server JAR ready!")
                    return@withContext Result.success(serverJarFile)
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }

        Result.failure(lastError ?: RuntimeException("Could not download Minecraft Beta 1.7.3 server jar"))
    }

    /**
     * Imports a user-selected custom server.jar
     */
    suspend fun importCustomJar(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open selected file stream"))

            val tempFile = File(serverDir, "custom_server.jar.tmp")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            if (tempFile.exists() && tempFile.length() > 10_000) {
                if (serverJarFile.exists()) serverJarFile.delete()
                tempFile.renameTo(serverJarFile)
                ensureEulaAgreed(true)
                Result.success(serverJarFile)
            } else {
                Result.failure(IllegalStateException("Imported file is empty or invalid"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Player management files helper
    fun readLinesSafe(file: File): List<String> {
        if (!file.exists()) return emptyList()
        return try {
            file.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addLine(file: File, line: String) {
        val current = readLinesSafe(file).toMutableList()
        val clean = line.trim()
        if (clean.isNotEmpty() && !current.contains(clean)) {
            current.add(clean)
            file.writeText(current.joinToString("\n") + "\n")
        }
    }

    fun removeLine(file: File, line: String) {
        val current = readLinesSafe(file).toMutableList()
        val clean = line.trim()
        if (current.remove(clean)) {
            file.writeText(current.joinToString("\n") + "\n")
        }
    }

    fun getOps(): List<String> = readLinesSafe(opsFile)
    fun addOp(name: String) = addLine(opsFile, name)
    fun removeOp(name: String) = removeLine(opsFile, name)

    fun getWhitelist(): List<String> = readLinesSafe(whitelistFile)
    fun addToWhitelist(name: String) = addLine(whitelistFile, name)
    fun removeFromWhitelist(name: String) = removeLine(whitelistFile, name)

    fun getBannedPlayers(): List<String> = readLinesSafe(bannedPlayersFile)
    fun banPlayer(name: String) = addLine(bannedPlayersFile, name)
    fun unbanPlayer(name: String) = removeLine(bannedPlayersFile, name)
}
