package com.remom.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remom.launcher.data.profile.GameProfile
import com.remom.launcher.ui.theme.RemomAccent
import com.remom.launcher.ui.theme.RemomSurface
import com.remom.launcher.ui.viewmodel.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(viewModel: LauncherViewModel) {
    val profiles by viewModel.profiles.collectAsState()
    val selectedId by viewModel.selectedProfileId.collectAsState()
    val versionsState by viewModel.versionsState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newVersion by remember { mutableStateOf("") }
    var newLoader by remember { mutableStateOf("Vanilla") }
    var versionExpanded by remember { mutableStateOf(false) }

    // Set default version when manifest loads
    LaunchedEffect(versionsState.latestRelease) {
        if (newVersion.isBlank() && versionsState.latestRelease != null) {
            newVersion = versionsState.latestRelease!!
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profiles",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    if (newVersion.isBlank()) {
                        newVersion = versionsState.latestRelease ?: "1.21.3"
                    }
                    showCreateDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = RemomAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Profile")
            }
        }

        if (versionsState.isLoading) {
            Text("Loading Minecraft versions...", style = MaterialTheme.typography.bodySmall)
        } else if (versionsState.releases.isNotEmpty()) {
            Text(
                "${versionsState.releases.size} release versions available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No profiles yet. Create one!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = profile.id == selectedId,
                        onSelect = { viewModel.selectProfile(profile.id) },
                        onPlay = { viewModel.selectProfile(profile.id) },
                        onDelete = { viewModel.deleteProfile(profile.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Profile") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Version dropdown from Mojang manifest
                    ExposedDropdownMenuBox(
                        expanded = versionExpanded,
                        onExpandedChange = { versionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = newVersion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Minecraft Version") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = versionExpanded,
                            onDismissRequest = { versionExpanded = false }
                        ) {
                            val versionList = if (versionsState.releases.isNotEmpty()) {
                                versionsState.releases
                            } else {
                                emptyList()
                            }
                            if (versionList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(if (versionsState.isLoading) "Loading..." else "No versions loaded") },
                                    onClick = {}
                                )
                            } else {
                                versionList.take(40).forEach { entry ->
                                    DropdownMenuItem(
                                        text = { Text("${entry.id} (${entry.type})") },
                                        onClick = {
                                            newVersion = entry.id
                                            versionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text("Loader", style = MaterialTheme.typography.labelMedium)
                    listOf("Vanilla", "Fabric", "Forge", "NeoForge", "OptiFine", "Quilt").forEach { loader ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = newLoader == loader,
                                onClick = { newLoader = loader }
                            )
                            Text(loader)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank() && newVersion.isNotBlank()) {
                        viewModel.addProfile(newName, newVersion, newLoader)
                        newName = ""
                        showCreateDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: GameProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val lastPlayedText = if (profile.lastPlayed > 0) dateFormat.format(Date(profile.lastPlayed)) else "Never"

    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) RemomAccent.copy(alpha = 0.25f) else RemomSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${profile.versionId} • ${profile.loader}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    "Last played: $lastPlayedText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = RemomAccent)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
