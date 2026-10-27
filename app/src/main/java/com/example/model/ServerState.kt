package com.example.model

sealed class ServerState {
    object Stopped : ServerState()
    
    data class DownloadingJar(val progress: Float, val statusMessage: String) : ServerState()
    
    data class InstallingJre(val progress: Float, val statusMessage: String) : ServerState()
    
    data class Starting(val message: String) : ServerState()
    
    data class Running(
        val startTimeMs: Long,
        val port: Int,
        val ipAddress: String,
        val processPid: Long? = null,
        val isCracked: Boolean = true,
        val jarName: String = "minecraft_server.jar"
    ) : ServerState()
    
    object Stopping : ServerState()
    
    data class Error(val message: String, val throwable: Throwable? = null) : ServerState()
}
