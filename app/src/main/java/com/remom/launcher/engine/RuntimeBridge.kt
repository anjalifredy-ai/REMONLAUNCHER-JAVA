package com.remom.launcher.engine

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.Surface
import com.remom.launcher.ui.GameActivity

/**
 * Bridge between the Kotlin launcher and the native Minecraft runtime.
 *
 * REALITY:
 * Running Minecraft Java on Android requires OpenJDK + GL translation + LWJGL natives.
 * That stack lives in Amethyst-Android / Pojav lineage (hundreds of thousands of lines).
 * Remom opens a real GameActivity (window + Surface). When the native backend is linked,
 * rendering goes into that Surface.
 */
interface RuntimeBridge {

    fun isRuntimeAvailable(): Boolean

    fun prepareRuntime(): Boolean

    fun startGame(
        activity: Activity,
        config: LaunchConfig,
        surface: Surface? = null
    ): Boolean

    fun stopGame()

    fun isGameRunning(): Boolean

    fun getLastError(): String?
}

/**
 * Opens the real GameActivity window and logs full launch config.
 * Does not execute Java bytecode until Amethyst JRE is linked.
 */
class StubRuntimeBridge : RuntimeBridge {

    private var running = false
    private var lastError: String? = null

    override fun isRuntimeAvailable(): Boolean = false

    override fun prepareRuntime(): Boolean = false

    override fun startGame(
        activity: Activity,
        config: LaunchConfig,
        surface: Surface?
    ): Boolean {
        Log.i(TAG, "======== REMOM LAUNCH CONFIG ========")
        Log.i(TAG, "Profile: ${config.profile.name} (${config.profile.versionId})")
        Log.i(TAG, "Loader: ${config.profile.loader}")
        Log.i(TAG, "Account: ${config.account.username} (${config.account.type})")
        Log.i(TAG, "Main class: ${config.mainClass}")
        Log.i(TAG, "Client JAR: ${config.clientJar.absolutePath} exists=${config.clientJar.exists()}")
        Log.i(TAG, "Game dir: ${config.gameDir.absolutePath}")
        Log.i(TAG, "Classpath: ${config.classpath.size} jars")
        Log.i(TAG, "JVM: ${config.jvmArgs.joinToString(" ")}")
        Log.i(TAG, "Game: ${config.gameArgs.joinToString(" ")}")
        Log.i(TAG, "=====================================")

        val msg = buildString {
            appendLine("Launch pipeline OK.")
            appendLine("Client JAR on disk: ${config.clientJar.exists()}")
            appendLine("Classpath jars: ${config.classpath.size}")
            appendLine()
            appendLine("JVM would run:")
            appendLine(config.mainClass)
            appendLine()
            appendLine("To actually play Minecraft here:")
            appendLine("Integrate AngelAuraMC/Amethyst-Android")
            appendLine("OpenJDK + MobileGL into this Surface.")
        }

        // Open real fullscreen game window
        val intent = Intent(activity, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_VERSION_ID, config.profile.versionId)
            putExtra(GameActivity.EXTRA_PROFILE_NAME, config.profile.name)
            putExtra(GameActivity.EXTRA_LOADER, config.profile.loader)
            putExtra(GameActivity.EXTRA_MAIN_CLASS, config.mainClass)
            putExtra(GameActivity.EXTRA_MESSAGE, msg)
        }
        activity.startActivity(intent)

        running = true
        lastError = null
        // Return true so UI shows "window opened" — game pixels need native runtime
        return true
    }

    override fun stopGame() {
        running = false
    }

    override fun isGameRunning(): Boolean = running

    override fun getLastError(): String? = lastError

    companion object {
        private const val TAG = "RuntimeBridge"
    }
}
