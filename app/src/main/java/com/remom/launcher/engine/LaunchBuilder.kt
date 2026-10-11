package com.remom.launcher.engine

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.remom.launcher.data.profile.AccountInfo
import com.remom.launcher.data.profile.GameProfile
import com.remom.launcher.data.profile.LauncherSettings
import java.io.File

/**
 * Builds a [LaunchConfig] from installed files + profile + account + settings.
 */
class LaunchBuilder(
    private val paths: GamePaths
) {
    private val gson = Gson()

    fun build(
        profile: GameProfile,
        account: AccountInfo,
        settings: LauncherSettings
    ): LaunchConfig {
        val versionId = profile.versionId
        val clientJar = paths.versionJar(versionId)
        val versionJsonFile = paths.versionJson(versionId)

        require(clientJar.exists()) { "Client JAR missing for $versionId — run installer first" }
        require(versionJsonFile.exists()) { "Version JSON missing for $versionId" }

        val json = gson.fromJson(versionJsonFile.readText(), JsonObject::class.java)
        val mainClass = json.get("mainClass")?.asString
            ?: "net.minecraft.client.main.Main"

        val classpath = buildClasspath(versionId, clientJar, json)
        val nativesDir = paths.nativesForVersion(versionId)
        val gameDir = paths.instanceDir(profile.id)

        val jvmArgs = buildJvmArgs(settings, nativesDir)
        val gameArgs = buildGameArgs(profile, account, gameDir, paths.assets, versionId)

        return LaunchConfig(
            profile = profile,
            account = account,
            settings = settings,
            gameDir = gameDir,
            assetsDir = paths.assets,
            librariesDir = paths.libraries,
            nativesDir = nativesDir,
            clientJar = clientJar,
            versionJson = versionJsonFile,
            classpath = classpath,
            mainClass = mainClass,
            jvmArgs = jvmArgs,
            gameArgs = gameArgs,
            renderer = settings.renderer
        )
    }

    private fun buildClasspath(versionId: String, clientJar: File, json: JsonObject): List<File> {
        val list = mutableListOf<File>()
        val libraries = json.getAsJsonArray("libraries") ?: return listOf(clientJar)

        for (el in libraries) {
            val obj = el.asJsonObject
            val name = obj.get("name")?.asString ?: continue
            val parts = name.split(":")
            if (parts.size < 3) continue
            val group = parts[0].replace('.', '/')
            val artifact = parts[1]
            val ver = parts[2]
            val file = File(paths.libraries, "$group/$artifact/$ver/$artifact-$ver.jar")
            if (file.exists()) list.add(file)
        }

        list.add(clientJar)
        return list
    }

    private fun buildJvmArgs(settings: LauncherSettings, nativesDir: File): List<String> {
        val args = mutableListOf<String>()
        args += "-Xmx${settings.allocatedRamMb}M"
        args += "-Xms512M"
        args += "-Djava.library.path=${nativesDir.absolutePath}"
        args += "-Dorg.lwjgl.opengl.libname=libgl4es.so" // placeholder for mobile GL

        // User custom args
        settings.jvmArgs.split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { args += it }

        return args
    }

    private fun buildGameArgs(
        profile: GameProfile,
        account: AccountInfo,
        gameDir: File,
        assetsDir: File,
        versionId: String
    ): List<String> {
        return listOf(
            "--username", account.username,
            "--version", versionId,
            "--gameDir", gameDir.absolutePath,
            "--assetsDir", assetsDir.absolutePath,
            "--assetIndex", versionId, // simplified; real index id comes from version JSON
            "--uuid", account.uuid ?: "00000000-0000-0000-0000-000000000000",
            "--accessToken", account.accessToken ?: "0",
            "--userType", if (account.type == "Microsoft") "msa" else "legacy",
            "--versionType", "remom"
        )
    }
}
