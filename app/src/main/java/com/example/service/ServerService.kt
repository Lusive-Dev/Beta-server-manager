package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.ServerState
import com.example.repository.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: ServerRepository

    override fun onCreate() {
        super.onCreate()
        repository = ServerRepository.getInstance(this)
        createNotificationChannel()

        serviceScope.launch {
            repository.serverState.collectLatest { state ->
                when (state) {
                    is ServerState.Running -> {
                        try {
                            startForeground(NOTIFICATION_ID, buildNotification(state))
                        } catch (e: Exception) {
                            // Run without foreground notification if permission not present
                        }
                    }
                    is ServerState.Stopped -> {
                        try {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } catch (e: Exception) {
                            // ignore
                        }
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            repository.stopServer()
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                // ignore
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val currentState = repository.serverState.value
        if (currentState is ServerState.Running) {
            try {
                startForeground(NOTIFICATION_ID, buildNotification(currentState))
            } catch (e: Exception) {
                // ignore
            }
        } else {
            repository.startServer()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(state: ServerState.Running): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val crackedLabel = if (state.isCracked) "Cracked: ON" else "Online"
        val pidLabel = state.processPid?.let { "PID: $it" } ?: "Process Active"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MC Beta 1.7 Server Running")
            .setContentText("${state.ipAddress}:${state.port} | $crackedLabel | $pidLabel")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MC Beta Server Running",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service notification for active Minecraft server"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "mc_beta_server_channel"
        const val NOTIFICATION_ID = 25565
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, ServerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
