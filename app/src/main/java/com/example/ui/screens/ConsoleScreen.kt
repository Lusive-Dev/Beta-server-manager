package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.ServerState
import com.example.ui.ServerViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalDim
import com.example.ui.theme.TerminalPrompt
import com.example.ui.theme.TerminalText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConsoleScreen(
    viewModel: ServerViewModel,
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var commandInput by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }

    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val isRunning = serverState is ServerState.Running
    val pid = (serverState as? ServerState.Running)?.processPid

    LaunchedEffect(logs.size, autoScroll) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    // Dynamic shell chips based on running vs offline shell
    val quickCommands = if (isRunning) {
        listOf("list", "help", "say Hello players!", "save-all", "time set 0", "stop")
    } else {
        listOf("ls -la", "cat server.properties", "java -version", "which java", "pwd", "cat eula.txt")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Terminal Status & Mode Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isRunning) EmeraldGreen else Slate400, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "PROCESS CONSOLE (PID: ${pid ?: "active"})" else "LINUX SHELL (/mc_server/)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) EmeraldLight else Slate400,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Auto Scroll Toggle
                Surface(
                    modifier = Modifier.clickable { autoScroll = !autoScroll },
                    color = if (autoScroll) EmeraldGreen.copy(alpha = 0.2f) else Slate800,
                    shape = RoundedCornerShape(4.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (autoScroll) EmeraldGreen else Slate700)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Auto-scroll",
                            tint = if (autoScroll) EmeraldLight else Slate400,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (autoScroll) "AUTO" else "MANUAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (autoScroll) EmeraldLight else Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Copy Logs
                IconButton(
                    onClick = {
                        val text = logs.joinToString("\n") { it.rawText }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Logs", text))
                        Toast.makeText(context, "Copied logs to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Logs",
                        tint = Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Clear Logs
                IconButton(
                    onClick = { viewModel.clearLogs() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear Logs",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Terminal Log Window
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Slate800, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF08090C))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .testTag("terminal_log_list")
            ) {
                items(logs, key = { it.id }) { log ->
                    TerminalLine(log = log)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Shell / Server Commands
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                AssistChip(
                    onClick = {
                        viewModel.sendCommand(cmd)
                    },
                    label = {
                        Text(
                            text = cmd,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Slate900,
                        labelColor = CyanAccent
                    ),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Slate700),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Interactive Command Bar
        val promptPrefix = if (isRunning) "mc> " else "sh$ "

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input"),
                placeholder = {
                    Text(
                        text = if (isRunning) "Enter Minecraft command (e.g. list, op, stop)..." else "Enter shell command (e.g. ls -la, which java)...",
                        color = TerminalDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                prefix = {
                    Text(
                        text = promptPrefix,
                        color = if (isRunning) EmeraldLight else TerminalPrompt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandInput.isNotBlank()) {
                            viewModel.sendCommand(commandInput)
                            commandInput = ""
                        }
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = if (isRunning) EmeraldGreen else CyanAccent,
                    unfocusedBorderColor = Slate700
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        viewModel.sendCommand(commandInput)
                        commandInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isRunning) EmeraldGreen else CyanAccent, RoundedCornerShape(10.dp))
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Command",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun TerminalLine(log: LogEntry) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val timeString = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    val textColor = when (log.level) {
        LogLevel.INFO -> TerminalText
        LogLevel.WARN -> AmberWarning
        LogLevel.SEVERE -> CrimsonError
        LogLevel.COMMAND -> CyanAccent
        LogLevel.SYSTEM -> Slate400
        LogLevel.DEBUG -> TerminalDim
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = "$timeString ",
            fontSize = 11.sp,
            color = TerminalDim,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = log.rawText,
            fontSize = 12.sp,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp
        )
    }
}
