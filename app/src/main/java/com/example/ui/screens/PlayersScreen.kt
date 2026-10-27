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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ServerViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900

@Composable
fun PlayersScreen(
    viewModel: ServerViewModel,
    connectedPlayers: List<String>,
    opsList: List<String>,
    whitelist: List<String>,
    bannedList: List<String>,
    modifier: Modifier = Modifier
) {
    var newOpName by remember { mutableStateOf("") }
    var newWhitelistName by remember { mutableStateOf("") }
    var newBanName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAYERS & ACCESS CONTROL",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Manage connected players, operators (ops.txt), whitelist, and bans.",
                fontSize = 12.sp,
                color = Slate400
            )
        }

        // 1. Live Connected Players Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONNECTED PLAYERS (${connectedPlayers.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            color = if (connectedPlayers.isNotEmpty()) EmeraldGreen.copy(alpha = 0.2f) else Slate800,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (connectedPlayers.isNotEmpty()) "ACTIVE" else "NO PLAYERS",
                                color = if (connectedPlayers.isNotEmpty()) EmeraldLight else Slate400,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (connectedPlayers.isEmpty()) {
                        Text(
                            text = "No players currently in-game. When players connect to your server IP, they will appear here.",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    } else {
                        connectedPlayers.forEach { player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = EmeraldGreen.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = EmeraldLight,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = player,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                Row {
                                    OutlinedButton(
                                        onClick = { viewModel.addOp(player) },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("OP", fontSize = 11.sp, color = CyanAccent)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.kickPlayer(player) },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("KICK", fontSize = 11.sp, color = CrimsonError)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Operators Card (ops.txt)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SERVER OPERATORS (ops.txt)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newOpName,
                            onValueChange = { newOpName = it },
                            placeholder = { Text("Player name...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_op_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate850,
                                unfocusedContainerColor = Slate850,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = Slate700
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newOpName.isNotBlank()) {
                                    viewModel.addOp(newOpName)
                                    newOpName = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            modifier = Modifier.testTag("add_op_button")
                        ) {
                            Text("ADD OP", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (opsList.isEmpty()) {
                        Text(text = "No server operators configured yet.", fontSize = 12.sp, color = Slate400)
                    } else {
                        opsList.forEach { op ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• $op", fontSize = 13.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                IconButton(onClick = { viewModel.removeOp(op) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Op", tint = Slate400)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Whitelist Card (white-list.txt)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WHITELIST (white-list.txt)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newWhitelistName,
                            onValueChange = { newWhitelistName = it },
                            placeholder = { Text("Player name...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate850,
                                unfocusedContainerColor = Slate850,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Slate700
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newWhitelistName.isNotBlank()) {
                                    viewModel.addToWhitelist(newWhitelistName)
                                    newWhitelistName = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("ADD", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (whitelist.isEmpty()) {
                        Text(text = "Whitelist is currently empty.", fontSize = 12.sp, color = Slate400)
                    } else {
                        whitelist.forEach { wl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• $wl", fontSize = 13.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                IconButton(onClick = { viewModel.removeFromWhitelist(wl) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Slate400)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Banned Players Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = CrimsonError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BANNED PLAYERS (banned-players.txt)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newBanName,
                            onValueChange = { newBanName = it },
                            placeholder = { Text("Player name...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate850,
                                unfocusedContainerColor = Slate850,
                                focusedBorderColor = CrimsonError,
                                unfocusedBorderColor = Slate700
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newBanName.isNotBlank()) {
                                    viewModel.banPlayer(newBanName)
                                    newBanName = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                        ) {
                            Text("BAN", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (bannedList.isEmpty()) {
                        Text(text = "No banned players.", fontSize = 12.sp, color = Slate400)
                    } else {
                        bannedList.forEach { ban ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• $ban", fontSize = 13.sp, color = CrimsonError, fontFamily = FontFamily.Monospace)
                                IconButton(onClick = { viewModel.unbanPlayer(ban) }) {
                                    Text("UNBAN", fontSize = 11.sp, color = CyanAccent)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
