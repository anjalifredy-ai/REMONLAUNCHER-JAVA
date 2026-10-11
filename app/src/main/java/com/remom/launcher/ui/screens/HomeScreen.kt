package com.remom.launcher.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.remom.launcher.engine.LaunchState
import com.remom.launcher.ui.theme.RemomAccent
import com.remom.launcher.ui.theme.RemomBluePrimary
import com.remom.launcher.ui.theme.RemomSurface
import com.remom.launcher.ui.viewmodel.LauncherViewModel

@Composable
fun HomeScreen(navController: NavController, viewModel: LauncherViewModel) {
    val profiles by viewModel.profiles.collectAsState()
    val selectedId by viewModel.selectedProfileId.collectAsState()
    val selectedProfile = profiles.find { it.id == selectedId }
    val accounts by viewModel.accounts.collectAsState()
    val launchState by viewModel.launchState.collectAsState()
    val launchMessage by viewModel.launchMessage.collectAsState()
    val context = LocalContext.current

    val isBusy = launchState == LaunchState.PREPARING || launchState == LaunchState.STARTING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "RemomLauncher",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = RemomBluePrimary
        )
        Text(
            text = "Minecraft Java Edition • Mobile",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RemomSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (launchState) {
                            LaunchState.PREPARING -> "Downloading..."
                            LaunchState.STARTING -> "Starting..."
                            LaunchState.RUNNING -> "Running"
                            LaunchState.ERROR -> "Error"
                            LaunchState.READY -> "Ready"
                            else -> if (selectedProfile != null) "Ready to Play" else "Select a Profile"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = selectedProfile?.let { "${it.name} • ${it.versionId} • ${it.loader}" }
                            ?: "Go to Profiles and create one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = launchMessage
                            ?: if (accounts.isNotEmpty()) "Account: ${accounts.first().username}" else "No account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (isBusy) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.9f))
                    }
                }

                Button(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            viewModel.play(activity)
                        }
                    },
                    enabled = selectedProfile != null && !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = RemomAccent),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isBusy) "..." else "PLAY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = "Profiles",
                subtitle = "${profiles.size} profiles",
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("profiles") }
            )
            FeatureCard(
                title = "Mods",
                subtitle = "Modrinth ready",
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("mods") }
            )
            FeatureCard(
                title = "Accounts",
                subtitle = "${accounts.size} accounts",
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("accounts") }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "v0.1.0-alpha • Engine: installer + launch pipeline live • Runtime backend next",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RemomSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
