package com.remom.launcher.data.profile

import com.google.gson.annotations.SerializedName

/**
 * Represents a single Minecraft instance / profile.
 */
data class GameProfile(
    val id: String,
    val name: String,
    val versionId: String,          // e.g. "1.21.3"
    val loader: String = "Vanilla", // Vanilla | Fabric | Forge | NeoForge | OptiFine | Quilt
    val loaderVersion: String? = null,
    val lastPlayed: Long = 0L,
    val icon: String? = null,
    val javaArgs: String? = null,
    val ramMb: Int = 2048,
    val gameDir: String? = null     // relative path under filesDir
)

data class ProfileList(
    val profiles: List<GameProfile> = emptyList(),
    val selectedProfileId: String? = null
)

data class AccountInfo(
    val id: String,
    val username: String,
    val type: String,               // "Offline" | "Microsoft"
    val uuid: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

data class AccountList(
    val accounts: List<AccountInfo> = emptyList(),
    val selectedAccountId: String? = null
)

data class LauncherSettings(
    val allocatedRamMb: Int = 2048,
    val javaRuntime: String = "Bundled (OpenJDK 21)",
    val renderer: String = "MobileGL",
    val jvmArgs: String = "-XX:+UseG1GC -XX:MaxGCPauseMillis=50",
    val curseForgeApiKey: String? = null
)
