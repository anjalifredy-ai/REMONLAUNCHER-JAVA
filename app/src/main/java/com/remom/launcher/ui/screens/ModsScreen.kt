package com.remom.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remom.launcher.data.modrinth.ModrinthHit
import com.remom.launcher.ui.theme.RemomAccent
import com.remom.launcher.ui.theme.RemomSurface
import com.remom.launcher.ui.viewmodel.LauncherViewModel
import java.io.File

@Composable
fun ModsScreen(viewModel: LauncherViewModel) {
    val modsState by viewModel.modsState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val selectedId by viewModel.selectedProfileId.collectAsState()
    val selectedProfile = profiles.find { it.id == selectedId }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Installed", "Modrinth", "CurseForge")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Mods & Content",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (selectedProfile != null) {
            Text(
                text = "Profile: ${selectedProfile.name} (${selectedProfile.versionId} • ${selectedProfile.loader})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search mods, resource packs, shaders...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                if (searchQuery.isNotBlank() && selectedTab == 1) {
                    IconButton(onClick = { viewModel.searchModrinth(searchQuery) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> InstalledModsList(
                files = modsState.installedMods,
                onDelete = { viewModel.deleteInstalledMod(it) }
            )
            1 -> ModrinthTab(
                query = searchQuery,
                state = modsState,
                onSearch = { viewModel.searchModrinth(it) },
                onDownload = { hit ->
                    viewModel.downloadMod(
                        hit,
                        gameVersion = selectedProfile?.versionId,
                        loader = selectedProfile?.loader?.lowercase()?.takeIf { it != "vanilla" }
                    )
                }
            )
            2 -> CurseForgePlaceholder()
        }
    }
}

@Composable
private fun InstalledModsList(
    files: List<File>,
    onDelete: (File) -> Unit
) {
    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No mods installed in current profile", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files, key = { it.absolutePath }) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = RemomSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(file.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDelete(file) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModrinthTab(
    query: String,
    state: com.remom.launcher.ui.viewmodel.ModsUiState,
    onSearch: (String) -> Unit,
    onDownload: (ModrinthHit) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (query.isNotBlank()) onSearch(query) },
                colors = ButtonDefaults.buttonColors(containerColor = RemomAccent),
                enabled = query.isNotBlank() && !state.isLoading
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.isLoading) "Searching..." else "Search Modrinth")
            }
            if (state.downloading != null) {
                Text("Downloading: ${state.downloading}", style = MaterialTheme.typography.bodySmall)
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.searchResults.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Type a query and press Search.\nMods download straight into the selected profile folder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.searchResults, key = { it.project_id }) { hit ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = RemomSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hit.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    hit.description.take(80) + if (hit.description.length > 80) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    "${hit.downloads} downloads • ${hit.author}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            IconButton(
                                onClick = { onDownload(hit) },
                                enabled = state.downloading == null
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Install", tint = RemomAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurseForgePlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CurseForge Integration", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Requires API key (add in Settings later).\nDownloads will go straight to the mods folder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
