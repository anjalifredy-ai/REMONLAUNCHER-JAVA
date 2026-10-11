package com.remom.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.remom.launcher.ui.navigation.RemomNavHost
import com.remom.launcher.ui.theme.RemomBackground
import com.remom.launcher.ui.theme.RemomLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemomLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RemomBackground
                ) {
                    RemomNavHost()
                }
            }
        }
    }
}
