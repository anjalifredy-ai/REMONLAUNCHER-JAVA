package com.remom.launcher.engine

import com.remom.launcher.data.profile.AccountInfo
import com.remom.launcher.data.profile.GameProfile
import com.remom.launcher.data.profile.LauncherSettings
import java.io.File

/**
 * Everything the runtime needs to start Minecraft.
 * Built by the launcher; consumed by the native/runtime backend.
 */
data class LaunchConfig(
    val profile: GameProfile,
    val account: AccountInfo,
    val settings: LauncherSettings,

    val gameDir: File,
    val assetsDir: File,
    val librariesDir: File,
    val nativesDir: File,
    val clientJar: File,
    val versionJson: File,

    /** Classpath entries in order */
    val classpath: List<File>,

    /** Main class from version JSON (e.g. net.minecraft.client.main.Main) */
    val mainClass: String,

    /** JVM arguments (including -Xmx from settings) */
    val jvmArgs: List<String>,

    /** Game arguments ( --username, --version, --gameDir, ... ) */
    val gameArgs: List<String>,

    /** Renderer hint for the mobile GL layer */
    val renderer: String = "MobileGL"
)

enum class LaunchState {
    IDLE,
    PREPARING,      // downloading jars/libs
    READY,
    STARTING,       // handing off to runtime
    RUNNING,
    STOPPED,
    ERROR
}
