package com.remom.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remom.launcher.ui.theme.RemomAccent

@Composable
fun SettingsScreen() {
    var ramMb by remember { mutableFloatStateOf(2048f) }
    var javaPath by remember { mutableStateOf("Bundled (OpenJDK 21)") }
    var renderer by remember { mutableStateOf("MobileGL") }
    var jvmArgs by remember { mutableStateOf("-XX:+UseG1GC -XX:MaxGCPauseMillis=50") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // RAM
        Text("Allocated RAM", style = MaterialTheme.typography.titleMedium)
        Text("${ramMb.toInt()} MB", style = MaterialTheme.typography.bodyLarge, color = RemomAccent)
        Slider(
            value = ramMb,
            onValueChange = { ramMb = it },
            valueRange = 512f..8192f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Recommended: 2–4 GB for most devices",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Java Runtime
        Text("Java Runtime", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        listOf("Bundled (OpenJDK 21)", "Bundled (OpenJDK 17)", "Custom Path").forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = javaPath == option,
                    onClick = { javaPath = option }
                )
                Text(option)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Renderer
        Text("Renderer", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        listOf("MobileGL", "GL4ES", "ANGLE", "VirGL").forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = renderer == option,
                    onClick = { renderer = option }
                )
                Text(option)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // JVM Arguments
        Text("JVM Arguments", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = jvmArgs,
            onValueChange = { jvmArgs = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            label = { Text("Custom JVM args") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Controls section placeholder
        Text("Controls", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: Open control editor */ }) {
            Text("Edit On-Screen Controls")
        }
        Text(
            "Pre-loaded layouts • Add buttons • Import / Export",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "More settings (skins, backgrounds, performance) coming soon.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
