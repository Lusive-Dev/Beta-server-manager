package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ServerConfig
import com.example.ui.ServerViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import kotlin.math.roundToInt

@Composable
fun ConfigScreen(
    viewModel: ServerViewModel,
    config: ServerConfig,
    modifier: Modifier = Modifier
) {
    var portText by remember(config.serverPort) { mutableStateOf(config.serverPort.toString()) }
    var isCracked by remember(config.onlineMode) { mutableStateOf(!config.onlineMode) }
    var autoEula by remember(config.autoEula) { mutableStateOf(config.autoEula) }
    var maxPlayers by remember(config.maxPlayers) { mutableIntStateOf(config.maxPlayers) }
    var levelName by remember(config.levelName) { mutableStateOf(config.levelName) }
    var levelSeed by remember(config.levelSeed) { mutableStateOf(config.levelSeed) }
    var motdText by remember(config.motd) { mutableStateOf(config.motd) }
    var memoryMb by remember(config.memoryMb) { mutableFloatStateOf(config.memoryMb.toFloat()) }
    var pvp by remember(config.pvp) { mutableStateOf(config.pvp) }
    var spawnMonsters by remember(config.spawnMonsters) { mutableStateOf(config.spawnMonsters) }
    var spawnAnimals by remember(config.spawnAnimals) { mutableStateOf(config.spawnAnimals) }
    var allowNether by remember(config.allowNether) { mutableStateOf(config.allowNether) }
    var whiteList by remember(config.whiteList) { mutableStateOf(config.whiteList) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SERVER.PROPERTIES CONFIG",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Edit game rules, network ports, authentication modes, and world settings.",
                fontSize = 12.sp,
                color = Slate400
            )
        }

        // Section 1: Authentication & EULA Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUTHENTICATION & AGREEMENTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Cracked Mode Switch
                    SettingSwitchRow(
                        title = "Cracked / Offline Mode",
                        subtitle = "Disables Mojang session check (online-mode=false) so beta & offline launchers connect without verification failure.",
                        checked = isCracked,
                        onCheckedChange = { isCracked = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto EULA Agreer Switch
                    SettingSwitchRow(
                        title = "Auto-Agree EULA (eula.txt)",
                        subtitle = "Automatically keeps 'eula=true' written in eula.txt to prevent JVM server halts.",
                        checked = autoEula,
                        onCheckedChange = { autoEula = it }
                    )
                }
            }
        }

        // Section 2: Network & Memory
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NETWORK & PERFORMANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = portText,
                            onValueChange = { portText = it },
                            label = { Text("Server Port") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("config_port_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate850,
                                unfocusedContainerColor = Slate850,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Slate700
                            )
                        )

                        OutlinedTextField(
                            value = maxPlayers.toString(),
                            onValueChange = { maxPlayers = it.toIntOrNull() ?: 20 },
                            label = { Text("Max Players") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("config_max_players_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate850,
                                unfocusedContainerColor = Slate850,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Slate700
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Memory Slider
                    Text(
                        text = "JVM Memory Allocation: ${memoryMb.roundToInt()} MB (-Xmx)",
                        fontSize = 13.sp,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = memoryMb,
                        onValueChange = { memoryMb = it },
                        valueRange = 256f..2048f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = CyanAccent,
                            inactiveTrackColor = Slate700
                        )
                    )
                }
            }
        }

        // Section 3: World & Gameplay Rules
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WORLD & GAMEPLAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = motdText,
                        onValueChange = { motdText = it },
                        label = { Text("MOTD (Server Description)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = Slate700
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = levelName,
                            onValueChange = { levelName = it },
                            label = { Text("Level Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate850,
                                unfocusedContainerColor = Slate850,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Slate700
                            )
                        )

                        OutlinedTextField(
                            value = levelSeed,
                            onValueChange = { levelSeed = it },
                            label = { Text("World Seed") },
                            placeholder = { Text("Optional") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate850,
                                unfocusedContainerColor = Slate850,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Slate700
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow("PVP Enabled", "Allow players to damage each other", pvp) { pvp = it }
                    SettingSwitchRow("Spawn Monsters", "Spawn hostile mobs (Zombies, Skeletons, Creepers)", spawnMonsters) { spawnMonsters = it }
                    SettingSwitchRow("Spawn Animals", "Spawn passive animals (Pigs, Cows, Sheep)", spawnAnimals) { spawnAnimals = it }
                    SettingSwitchRow("Allow Nether", "Enable Nether portal generation", allowNether) { allowNether = it }
                    SettingSwitchRow("Whitelist Enabled", "Only allow players in white-list.txt", whiteList) { whiteList = it }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 25565
                    val updated = config.copy(
                        serverPort = port,
                        onlineMode = !isCracked,
                        autoEula = autoEula,
                        maxPlayers = maxPlayers,
                        levelName = levelName.ifBlank { "world" },
                        levelSeed = levelSeed,
                        motd = motdText,
                        memoryMb = memoryMb.roundToInt(),
                        pvp = pvp,
                        spawnMonsters = spawnMonsters,
                        spawnAnimals = spawnAnimals,
                        allowNether = allowNether,
                        whiteList = whiteList
                    )
                    viewModel.updateConfig(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_config_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE & APPLY CONFIG",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Slate400,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldLight,
                checkedTrackColor = EmeraldDark
            )
        )
    }
}
