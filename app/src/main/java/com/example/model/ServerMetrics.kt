package com.example.model

/**
 * Real, verifiable system and process metrics for the Minecraft server.
 * No fabricated or simulated numbers.
 */
data class ServerMetrics(
    val isRunning: Boolean = false,
    val processPid: Long? = null,
    val uptimeSeconds: Long = 0L,
    val formattedUptime: String = "00:00:00",
    val usedRamMb: Int = 0,
    val maxConfiguredRamMb: Int = 512,
    val hasServerJar: Boolean = false,
    val serverJarSizeBytes: Long = 0L,
    val serverJarSizeFormatted: String = "Missing",
    val worldFolderSizeBytes: Long = 0L,
    val worldFolderSizeFormatted: String = "0 KB",
    val serverPort: Int = 25565,
    val lastExitCode: Int? = null
)
