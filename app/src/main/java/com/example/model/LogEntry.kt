package com.example.model

enum class LogLevel {
    INFO,
    WARN,
    SEVERE,
    COMMAND,
    SYSTEM,
    DEBUG
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String,
    val level: LogLevel = LogLevel.INFO,
    val sender: String = "Server"
) {
    companion object {
        fun parse(line: String): LogEntry {
            val trimmed = line.trimEnd()
            val lower = trimmed.lowercase()
            val level = when {
                lower.contains("[severe]") || lower.contains("error") || lower.contains("exception") -> LogLevel.SEVERE
                lower.contains("[warning]") || lower.contains("warn") -> LogLevel.WARN
                trimmed.startsWith(">") || trimmed.startsWith("$") -> LogLevel.COMMAND
                trimmed.startsWith("[system]") -> LogLevel.SYSTEM
                else -> LogLevel.INFO
            }
            return LogEntry(rawText = trimmed, level = level)
        }
    }
}
