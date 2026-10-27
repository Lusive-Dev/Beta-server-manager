package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ServerConfig
import com.example.model.ServerState
import com.example.ui.ServerViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ServerMetricsCard
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: ServerViewModel,
    serverState: ServerState,
    config: ServerConfig,
    localIp: String,
    onNavigateToConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metrics by viewModel.serverMetrics.collectAsStateWithLifecycle()
    val hasJar = viewModel.hasServerJar()
    val jarSize = viewModel.getServerJarSize()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importCustomJar(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ServerStatusHeader(serverState = serverState, config = config)
        }

        item {
            // Main Server Control Action (Start / Stop)
            ServerActionCard(
                serverState = serverState,
                hasJar = hasJar,
                onStart = { viewModel.startServer() },
                onStop = { viewModel.stopServer() },
                onDownloadJar = { viewModel.downloadOfficialJar() }
            )
        }

        item {
            // Key Highlight: Cracked / Offline Mode Toggle & Auto-EULA
            QuickTogglesCard(
                config = config,
                onToggleCracked = { viewModel.toggleCrackedMode(it) },
                onToggleAutoEula = { viewModel.toggleAutoEula(it) }
            )
        }

        item {
            // Network Address & Connection Info Card
            ConnectionCard(
                serverState = serverState,
                localIp = localIp,
                port = config.serverPort,
                onCopyAddress = { address ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Server Address", address))
                    Toast.makeText(context, "Copied $address to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }

        item {
            // Real-Time Server Metrics Card (Active Players, Uptime, RAM, CPU/TPS)
            ServerMetricsCard(
                metrics = metrics,
                onNavigateToConsole = onNavigateToConsole
            )
        }

        item {
            // JAR Management Card
            JarManagementCard(
                hasJar = hasJar,
                jarSize = jarSize,
                serverState = serverState,
                onDownloadJar = { viewModel.downloadOfficialJar() },
                onImportJar = { filePickerLauncher.launch(arrayOf("*/*", "application/java-archive")) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ServerStatusHeader(
    serverState: ServerState,
    config: ServerConfig
) {
    val isRunning = serverState is ServerState.Running
    val isStarting = serverState is ServerState.Starting || serverState is ServerState.DownloadingJar
    val isError = serverState is ServerState.Error

    val dotColor = when {
        isRunning -> EmeraldGreen
        isStarting -> AmberWarning
        isError -> CrimsonError
        else -> Slate400
    }

    val stateText = when (serverState) {
        is ServerState.Running -> "SERVER RUNNING"
        is ServerState.Starting -> "BOOTING SERVER..."
        is ServerState.DownloadingJar -> "DOWNLOADING JAR..."
        is ServerState.Stopping -> "STOPPING SERVER..."
        is ServerState.Error -> "SERVER ERROR"
        else -> "SERVER OFFLINE"
    }

    // Infinite pulsing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server_status_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isRunning) EmeraldDark else Slate700)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(if (isRunning || isStarting) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stateText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = dotColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = if (!config.onlineMode) EmeraldDark.copy(alpha = 0.3f) else Slate800,
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (!config.onlineMode) EmeraldGreen else Slate700)
                    )
                ) {
                    Text(
                        text = if (!config.onlineMode) "CRACKED ON" else "PREMIUM",
                        color = if (!config.onlineMode) EmeraldLight else Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (serverState is ServerState.Running) {
                Spacer(modifier = Modifier.height(10.dp))
                UptimeDisplay(startTimeMs = serverState.startTimeMs)
            } else if (serverState is ServerState.DownloadingJar) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = serverState.statusMessage,
                    fontSize = 13.sp,
                    color = AmberWarning
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { serverState.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = EmeraldGreen,
                    trackColor = Slate800
                )
            } else if (serverState is ServerState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = serverState.message,
                    fontSize = 13.sp,
                    color = CrimsonError
                )
            }
        }
    }
}

@Composable
fun UptimeDisplay(startTimeMs: Long) {
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(startTimeMs) {
        while (true) {
            delay(1000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val elapsedSeconds = ((currentTimeMs - startTimeMs) / 1000).coerceAtLeast(0)
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val uptimeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "UPTIME: ",
            fontSize = 12.sp,
            color = Slate400,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = uptimeStr,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = EmeraldLight,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ServerActionCard(
    serverState: ServerState,
    hasJar: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDownloadJar: () -> Unit
) {
    val isRunning = serverState is ServerState.Running
    val isStarting = serverState is ServerState.Starting || serverState is ServerState.DownloadingJar

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate850)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isRunning) {
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("stop_server_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Server",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STOP SERVER",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            } else if (isStarting) {
                Button(
                    onClick = { /* busy */ },
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = Slate800)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = EmeraldGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "STARTING UP...",
                        fontSize = 15.sp,
                        color = Slate400
                    )
                }
            } else if (!hasJar) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "minecraft_server.jar is missing. Download Beta 1.7.3 to start.",
                            color = AmberWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onDownloadJar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_server_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download Server Jar",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DOWNLOAD BETA 1.7.3 (1.4 MB)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_server_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Server",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START SERVER",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickTogglesCard(
    config: ServerConfig,
    onToggleCracked: (Boolean) -> Unit,
    onToggleAutoEula: (Boolean) -> Unit
) {
    val isCracked = !config.onlineMode

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Slate700)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SERVER ESSENTIALS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Cracked Mode Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCracked(!isCracked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cracked Mode (Offline)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCracked) EmeraldLight else Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isCracked) EmeraldGreen else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Allows offline & legacy clients without Mojang account verification (online-mode=false).",
                        fontSize = 12.sp,
                        color = Slate400,
                        lineHeight = 16.sp
                    )
                }

                Switch(
                    checked = isCracked,
                    onCheckedChange = onToggleCracked,
                    modifier = Modifier.testTag("cracked_mode_toggle"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldLight,
                        checkedTrackColor = EmeraldDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-EULA Agreer Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleAutoEula(!config.autoEula) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Auto EULA Agreer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (config.autoEula) EmeraldGreen else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Automatically writes 'eula=true' into eula.txt so server never crashes on startup.",
                        fontSize = 12.sp,
                        color = Slate400,
                        lineHeight = 16.sp
                    )
                }

                Switch(
                    checked = config.autoEula,
                    onCheckedChange = onToggleAutoEula,
                    modifier = Modifier.testTag("auto_eula_toggle"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldLight,
                        checkedTrackColor = EmeraldDark
                    )
                )
            }
        }
    }
}

@Composable
fun ConnectionCard(
    serverState: ServerState,
    localIp: String,
    port: Int,
    onCopyAddress: (String) -> Unit
) {
    val address = "$localIp:$port"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Slate700)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "WiFi",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOCAL LAN CONNECTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "UDP LAN PING ON",
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyAddress(address) },
                color = Slate950,
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Slate800)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SERVER ADDRESS",
                            fontSize = 10.sp,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = address,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = { onCopyAddress(address) },
                        modifier = Modifier.testTag("copy_ip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy IP",
                            tint = CyanAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Join in Minecraft Beta 1.7.3 client under Multiplayer using this address.",
                fontSize = 12.sp,
                color = Slate400
            )
        }
    }
}



@Composable
fun JarManagementCard(
    hasJar: Boolean,
    jarSize: String,
    serverState: ServerState,
    onDownloadJar: () -> Unit,
    onImportJar: () -> Unit
) {
    val isDownloading = serverState is ServerState.DownloadingJar

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SERVER JAR SOURCE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Surface(
                    color = if (hasJar) EmeraldDark.copy(alpha = 0.3f) else CrimsonError.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (hasJar) "READY ($jarSize)" else "NOT FOUND",
                        color = if (hasJar) EmeraldLight else CrimsonError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Beta 1.7.3 server executable runs the classic game world. You can download the official 1.4MB archive or import your own modded/bukkit jar.",
                fontSize = 12.sp,
                color = Slate400,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDownloadJar,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("download_official_jar_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Download",
                        tint = EmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDownloading) "DOWNLOADING..." else "AUTO-DOWNLOAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }

                OutlinedButton(
                    onClick = onImportJar,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_custom_jar_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Import",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IMPORT JAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }
            }
        }
    }
}
