package com.example

import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.ServerConfig
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testServerConfigPropertiesSerialization() {
        val config = ServerConfig(
            serverPort = 25565,
            onlineMode = false, // Cracked
            levelName = "BetaWorld",
            maxPlayers = 16
        )

        val propertiesString = config.toPropertiesFileContent()

        assertTrue(propertiesString.contains("server-port=25565"))
        assertTrue(propertiesString.contains("online-mode=false"))
        assertTrue(propertiesString.contains("level-name=BetaWorld"))
        assertTrue(propertiesString.contains("max-players=16"))
    }

    @Test
    fun testLogEntryParsing() {
        val infoLog = LogEntry.parse("2026-10-27 12:00:00 [INFO] Starting minecraft server version Beta 1.7.3")
        assertEquals(LogLevel.INFO, infoLog.level)

        val severeLog = LogEntry.parse("[SEVERE] java.net.BindException: Address already in use")
        assertEquals(LogLevel.SEVERE, severeLog.level)

        val warnLog = LogEntry.parse("[WARNING] Can't keep up! Did the system time change?")
        assertEquals(LogLevel.WARN, warnLog.level)

        val commandLog = LogEntry.parse("> say Hello")
        assertEquals(LogLevel.COMMAND, commandLog.level)
    }

    @Test
    fun testServerMetricsDefaultValues() {
        val metrics = com.example.model.ServerMetrics(
            isRunning = true,
            processPid = 1234L,
            uptimeSeconds = 3600L,
            formattedUptime = "01:00:00",
            usedRamMb = 128,
            maxConfiguredRamMb = 512,
            hasServerJar = true,
            serverJarSizeBytes = 1450000L,
            serverJarSizeFormatted = "1.38 MB",
            serverPort = 25565
        )

        assertEquals("01:00:00", metrics.formattedUptime)
        assertEquals(128, metrics.usedRamMb)
        assertEquals(1234L, metrics.processPid)
        assertTrue(metrics.hasServerJar)
        assertTrue(metrics.isRunning)
    }
}
