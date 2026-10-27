package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class LanBroadcastManager(private val context: Context) {

    private var broadcastJob: Job? = null
    private var socket: DatagramSocket? = null

    /**
     * Finds the local IPv4 address (e.g., 192.168.1.50)
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    /**
     * Broadcasts MOTD packets across the local subnet on UDP port 4445
     * so Minecraft LAN multiplayer discovery finds the server.
     */
    fun startBroadcasting(scope: CoroutineScope, motd: String, port: Int) {
        stopBroadcasting()
        broadcastJob = scope.launch(Dispatchers.IO) {
            try {
                val broadcastSocket = DatagramSocket().apply {
                    broadcast = true
                }
                socket = broadcastSocket

                val broadcastMessage = "[MOTD]$motd[/MOTD][AD]$port[/AD]"
                val bytes = broadcastMessage.toByteArray(Charsets.UTF_8)
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(bytes, bytes.size, broadcastAddress, 4445)

                while (isActive) {
                    try {
                        broadcastSocket.send(packet)
                    } catch (e: Exception) {
                        // ignore dropped broadcast
                    }
                    delay(1500)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopBroadcasting() {
        broadcastJob?.cancel()
        broadcastJob = null
        try {
            socket?.close()
        } catch (e: Exception) {
            // ignore
        }
        socket = null
    }
}
