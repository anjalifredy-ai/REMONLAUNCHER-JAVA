package com.remom.launcher.ui

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Fullscreen game window host.
 *
 * This Activity is the real "game window".
 * - SurfaceView provides the rendering surface for the native GL layer
 * - When RuntimeBridge is linked to Amethyst/Mojo, the JVM renders into this surface
 * - Until then, shows status so the pipeline is testable end-to-end
 */
class GameActivity : ComponentActivity(), SurfaceHolder.Callback {

    private var surfaceView: SurfaceView? = null
    private var statusText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive fullscreen landscape-friendly
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { c ->
            c.hide(WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val versionId = intent.getStringExtra(EXTRA_VERSION_ID) ?: "unknown"
        val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Profile"
        val loader = intent.getStringExtra(EXTRA_LOADER) ?: "Vanilla"
        val mainClass = intent.getStringExtra(EXTRA_MAIN_CLASS) ?: ""
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF0A1628.toInt())
        }

        surfaceView = SurfaceView(this).apply {
            holder.addCallback(this@GameActivity)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(surfaceView)

        statusText = TextView(this).apply {
            setTextColor(0xFFE8F0FE.toInt())
            textSize = 14f
            setPadding(48, 48, 48, 48)
            text = buildString {
                appendLine("Remom Game Window")
                appendLine()
                appendLine("Profile: $profileName")
                appendLine("Version: $versionId")
                appendLine("Loader: $loader")
                appendLine("Main: $mainClass")
                appendLine()
                appendLine(message.ifBlank {
                    "Surface ready. Runtime backend not linked yet.\n" +
                        "Official game files are on disk.\n" +
                        "Next: link Amethyst/Mojo JRE + GL to this Surface."
                })
                appendLine()
                appendLine("Back = exit")
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(statusText)

        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.i(TAG, "Game window closed by user")
                finish()
            }
        })
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "Surface created — ready for GL/runtime binding")
        // When RuntimeBridge is real:
        // runtime.attachSurface(holder.surface)
        // runtime.resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i(TAG, "Surface changed: ${width}x$height")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "Surface destroyed")
        // runtime.detachSurface()
    }

    companion object {
        private const val TAG = "GameActivity"
        const val EXTRA_VERSION_ID = "version_id"
        const val EXTRA_PROFILE_NAME = "profile_name"
        const val EXTRA_LOADER = "loader"
        const val EXTRA_MAIN_CLASS = "main_class"
        const val EXTRA_MESSAGE = "message"
    }
}
