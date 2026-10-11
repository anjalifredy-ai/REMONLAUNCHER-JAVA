package com.remom.launcher.engine

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.Surface
import com.remom.launcher.ui.GameActivity

/**
 * Runtime bridge aimed at Amethyst / Pojav lineage.
 *
 * When Amethyst is installed as a separate app → try external launch with paths.
 * When Amethyst modules are compiled into this APK → replace reflection section
 * with direct calls to net.kdt.pojavlaunch.utils.JREUtils.launchJavaVM(...).
 *
 * This file is the single place to upgrade from "window only" → "playable".
 */
class AmethystRuntimeBridge : RuntimeBridge {

    private var running = false
    private var lastError: String? = null

    /** Known Amethyst / Pojav package ids (may vary by fork) */
    private val amethystPackages = listOf(
        "net.kdt.pojavlaunch",
        "net.kdt.pojavlaunch.debug",
        "org.angelauramc.amethyst",
        "org.angelauramc.amethyst.debug"
    )

    override fun isRuntimeAvailable(): Boolean {
        // True only when native JRE is bundled or Amethyst app present
        return false // flip when submodule linked or external app detected at runtime
    }

    override fun prepareRuntime(): Boolean {
        // Extract JRE / check MultiRT — implemented after Amethyst module link
        return false
    }

    override fun startGame(
        activity: Activity,
        config: LaunchConfig,
        surface: Surface?
    ): Boolean {
        logConfig(config)

        // 1) If Amethyst app installed — open it (user plays there with same files if shared storage)
        if (tryLaunchExternalAmethyst(activity, config)) {
            running = true
            return true
        }

        // 2) If in-process Amethyst classes exist (after Gradle merge)
        if (tryLaunchInProcess(activity, config)) {
            running = true
            return true
        }

        // 3) Fallback: Remom GameActivity (surface host + instructions)
        openGameWindow(activity, config)
        running = true
        lastError = null
        return true
    }

    override fun stopGame() {
        running = false
    }

    override fun isGameRunning(): Boolean = running

    override fun getLastError(): String? = lastError

    private fun tryLaunchExternalAmethyst(activity: Activity, config: LaunchConfig): Boolean {
        val pm = activity.packageManager
        for (pkg in amethystPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                val launch = pm.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // Cannot fully inject classpath into foreign app without shared API;
                    // user continues in Amethyst. Remom still owns downloads/UI.
                    activity.startActivity(launch)
                    Log.i(TAG, "Opened external Amethyst/Pojav: $pkg")
                    lastError = "Opened $pkg — play there until in-process runtime is linked."
                    return true
                }
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return false
    }

    /**
     * After :app_pojavlauncher is a dependency, implement:
     *
     *   JREUtils.launchJavaVM(activity, runtime, null, javaArgList, customArgs)
     *
     * using config.classpath, config.jvmArgs, config.gameArgs, config.mainClass.
     */
    private fun tryLaunchInProcess(activity: Activity, config: LaunchConfig): Boolean {
        return try {
            val jreUtils = Class.forName("net.kdt.pojavlaunch.utils.JREUtils")
            // Class present → project was merged; full invoke needs Runtime object from MultiRTUtils
            Log.i(TAG, "JREUtils found — wire MultiRTUtils + launchJavaVM next")
            lastError = "Amethyst classes detected. Complete MultiRT + launchJavaVM wiring."
            false
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    private fun openGameWindow(activity: Activity, config: LaunchConfig) {
        val msg = buildString {
            appendLine("Remom → Amethyst bridge")
            appendLine()
            appendLine("Client: ${config.clientJar.name} (${if (config.clientJar.exists()) "OK" else "MISSING"})")
            appendLine("Classpath: ${config.classpath.size} jars")
            appendLine("Loader: ${config.profile.loader}")
            appendLine("RAM: look at -Xmx in JVM args")
            appendLine()
            appendLine("PLAYABLE path:")
            appendLine("1. Build Amethyst-Android (see docs/AMETHYST_INTEGRATION.md)")
            appendLine("2. Or install Amethyst APK alongside Remom")
            appendLine("3. Or submodule + JREUtils.launchJavaVM")
            appendLine()
            appendLine("Worlds & servers work inside Amethyst engine,")
            appendLine("not inside this placeholder surface.")
        }

        val intent = Intent(activity, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_VERSION_ID, config.profile.versionId)
            putExtra(GameActivity.EXTRA_PROFILE_NAME, config.profile.name)
            putExtra(GameActivity.EXTRA_LOADER, config.profile.loader)
            putExtra(GameActivity.EXTRA_MAIN_CLASS, config.mainClass)
            putExtra(GameActivity.EXTRA_MESSAGE, msg)
        }
        activity.startActivity(intent)
    }

    private fun logConfig(config: LaunchConfig) {
        Log.i(TAG, "Launch ${config.profile.name} ${config.profile.versionId} loader=${config.profile.loader}")
        Log.i(TAG, "main=${config.mainClass} jars=${config.classpath.size}")
        Log.i(TAG, "jvm=${config.jvmArgs.joinToString(" ")}")
    }

    companion object {
        private const val TAG = "AmethystRuntime"

        /** Mobile-friendly JVM extras (append in LaunchBuilder / settings) */
        fun optimizedJvmExtras(ramMb: Int): List<String> = listOf(
            "-Xms256M",
            "-Xmx${ramMb.coerceIn(512, 4096)}M",
            "-XX:+UseG1GC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:MaxGCPauseMillis=50",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:G1NewSizePercent=20",
            "-XX:G1ReservePercent=20",
            "-XX:G1HeapRegionSize=32M",
            "-Djava.awt.headless=false"
        )
    }
}
