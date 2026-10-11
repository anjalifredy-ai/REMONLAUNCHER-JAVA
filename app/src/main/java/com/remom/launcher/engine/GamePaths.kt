package com.remom.launcher.engine

import android.content.Context
import java.io.File

/**
 * Central path manager for the Remom engine.
 * Mirrors a standard Minecraft directory layout under app private storage.
 */
class GamePaths(private val context: Context) {

    /** Root: filesDir/remom/ */
    val root: File = File(context.filesDir, "remom").also { it.mkdirs() }

    /** Shared libraries cache (across profiles) */
    val libraries: File = File(root, "libraries").also { it.mkdirs() }

    /** Shared assets */
    val assets: File = File(root, "assets").also { it.mkdirs() }

    /** Version jars + version json cache */
    val versions: File = File(root, "versions").also { it.mkdirs() }

    /** Per-profile game directories */
    val instances: File = File(root, "instances").also { it.mkdirs() }

    /** Native libs extracted for current ABI */
    val natives: File = File(root, "natives").also { it.mkdirs() }

    /** Runtime / JRE root (bundled or user-provided) */
    val runtime: File = File(root, "runtime").also { it.mkdirs() }

    fun versionDir(versionId: String): File =
        File(versions, versionId).also { it.mkdirs() }

    fun versionJar(versionId: String): File =
        File(versionDir(versionId), "$versionId.jar")

    fun versionJson(versionId: String): File =
        File(versionDir(versionId), "$versionId.json")

    fun instanceDir(profileId: String): File =
        File(instances, profileId).also { it.mkdirs() }

    fun instanceMods(profileId: String): File =
        File(instanceDir(profileId), "mods").also { it.mkdirs() }

    fun instanceResourcePacks(profileId: String): File =
        File(instanceDir(profileId), "resourcepacks").also { it.mkdirs() }

    fun instanceShaderPacks(profileId: String): File =
        File(instanceDir(profileId), "shaderpacks").also { it.mkdirs() }

    fun instanceSaves(profileId: String): File =
        File(instanceDir(profileId), "saves").also { it.mkdirs() }

    fun nativesForVersion(versionId: String): File =
        File(natives, versionId).also { it.mkdirs() }
}
