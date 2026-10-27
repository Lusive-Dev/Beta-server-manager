package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ServerMetrics
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

@Composable
fun ServerMetricsCard(
    metrics: ServerMetrics,
    onNavigateToConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = metrics.isRunning

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("server_metrics_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isRunning) EmeraldDark else Slate700
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(if (isRunning) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(if (isRunning) EmeraldGreen else Slate400)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PROCESS & SYSTEM METRICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isRunning) EmeraldGreen.copy(alpha = 0.2f) else Slate800,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isRunning) "ACTIVE" else "OFFLINE",
                            color = if (isRunning) EmeraldLight else Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = onNavigateToConsole,
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("metrics_jump_to_console_button"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Console",
                            modifier = Modifier.size(13.dp),
                            tint = EmeraldLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SHELL",
                            fontSize = 10.sp,
                            color = EmeraldLight,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2x2 Grid of Genuine Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Real Process & Uptime Metric Tile
                MetricTile(
                    title = "PROCESS PID",
                    value = if (isRunning) "PID ${metrics.processPid ?: "Active"}" else "OFFLINE",
                    subtitle = if (isRunning) "Uptime: ${metrics.formattedUptime}" else "Exit: ${metrics.lastExitCode ?: 0}",
                    progress = if (isRunning) 1.0f else 0f,
                    progressColor = if (isRunning) EmeraldGreen else Slate700,
                    icon = Icons.Default.Schedule,
                    iconTint = if (isRunning) EmeraldLight else Slate400,
                    testTag = "metric_uptime",
                    modifier = Modifier.weight(1f)
                )

                // 2. Real Server JAR Metric Tile
                MetricTile(
                    title = "SERVER.JAR",
                    value = if (metrics.hasServerJar) metrics.serverJarSizeFormatted else "MISSING",
                    subtitle = if (metrics.hasServerJar) "b1.7.3 Present" else "Download Required",
                    progress = if (metrics.hasServerJar) 1f else 0f,
                    progressColor = if (metrics.hasServerJar) CyanAccent else CrimsonError,
                    icon = Icons.Default.Dns,
                    iconTint = if (metrics.hasServerJar) CyanAccent else CrimsonError,
                    testTag = "metric_players",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3. Real RAM Usage Metric Tile
                val ramRatio = (metrics.usedRamMb.toFloat() / metrics.maxConfiguredRamMb.coerceAtLeast(128)).coerceIn(0f, 1f)
                val animatedRamProgress by animateFloatAsState(
                    targetValue = ramRatio,
                    animationSpec = tween(400),
                    label = "ramProgress"
                )

                MetricTile(
                    title = "APP MEMORY",
                    value = "${metrics.usedRamMb} MB",
                    subtitle = "Allocated: ${metrics.maxConfiguredRamMb} MB",
                    progress = animatedRamProgress,
                    progressColor = if (ramRatio > 0.85f) AmberWarning else CyanAccent,
                    icon = Icons.Default.Memory,
                    iconTint = CyanAccent,
                    testTag = "metric_ram",
                    modifier = Modifier.weight(1f)
                )

                // 4. Real World Directory Metric Tile
                MetricTile(
                    title = "WORLD FOLDER",
                    value = metrics.worldFolderSizeFormatted,
                    subtitle = "Port: ${metrics.serverPort}",
                    progress = 1.0f,
                    progressColor = EmeraldLight,
                    icon = Icons.Default.Folder,
                    iconTint = EmeraldLight,
                    testTag = "metric_cpu",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    progressColor: Color,
    icon: ImageVector,
    iconTint: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag(testTag),
        color = Slate850,
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Slate800)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Slate400,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = Slate800
            )
        }
    }
}
