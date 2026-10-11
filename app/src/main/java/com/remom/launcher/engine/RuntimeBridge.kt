package com.remom.launcher.engine

import android.app.Activity
import android.util.Log
import android.view.Surface

/**
 * Bridge between the Kotlin launcher and the native Minecraft runtime.
 *
 * REALITY:
 * Running Minecraft Java on Android requires:
 * 1. OpenJDK / mobile JRE (ARM64)
 * 2. OpenGL ES translation layer (MobileGL / GL4ES / ANGLE)
 * 3. LWJGL natives patched for Android
 * 4. Input (touch → mouse/keyboard) pipeline
 * 5. Audio (OpenAL)
 *
 * These pieces live in projects like Amethyst, MojoLauncher, FoldCraftLauncher.
 * RemomLauncher will integrate one of those backends rather than reinventing them.
 *
 * This class is the contract the rest of the app talks to.
 * When the native backend is linked, implement the methods with JNI / AIDL.
 */
interface RuntimeBridge {

    /** Whether a usable runtime (JRE + GL) is installed on device */
    fun isRuntimeAvailable(): Boolean

    /** Prepare natives / extract if needed */
    fun prepareRuntime(): Boolean

    /**
     * Start the game process with the given config.
     * @param activity host activity (for surface / lifecycle)
     * @param surface optional Surface for rendering (when using dedicated view)
     */
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
 * Stub implementation until Amethyst/Mojo-style backend is integrated.
 * Logs the full launch command so we can verify the pipeline end-to-end.
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
        Log.i(TAG, "Account: ${config.account.username} (${config.account.type})")
        Log.i(TAG, "Main class: ${config.mainClass}")
        Log.i(TAG, "Client JAR: ${config.clientJar.absolutePath}")
        Log.i(TAG, "Game dir: ${config.gameDir.absolutePath}")
        Log.i(TAG, "Natives: ${config.nativesDir.absolutePath}")
        Log.i(TAG, "Renderer: ${config.renderer}")
        Log.i(TAG, "Classpath entries: ${config.classpath.size}")
        Log.i(TAG, "JVM: ${config.jvmArgs.joinToString(" ")}")
        Log.i(TAG, "Game: ${config.gameArgs.joinToString(" ")}")
        Log.i(TAG, "=====================================")

        lastError = "Runtime backend not linked yet. " +
            "Install pipeline works — next step is integrating Amethyst/Mojo JRE + GL layer."
        running = false
        return false
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
