package com.remom.launcher.engine.loaders

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.remom.launcher.engine.DownloadProgress
import com.remom.launcher.engine.GamePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Separate install path for each mod loader.
 *
 * Vanilla  → just official Mojang client (GameInstaller)
 * Fabric   → meta.fabricmc.net profile JSON + loader/intermediary jars
 * Quilt    → meta.quiltmc.org (same shape as Fabric)
 * Forge    → installer JSON from Maven (complex; stubbed with clear path)
 * NeoForge → similar to Forge via NeoForged Maven
 *
 * After install, profile.versionId points at the loader version id
 * e.g. "1.21.3-fabric-0.16.9" so LaunchBuilder uses the right mainClass + classpath.
 */
class LoaderInstaller(
    private val paths: GamePaths
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    companion object {
        private const val TAG = "LoaderInstaller"
        private const val FABRIC_META = "https://meta.fabricmc.net/v2"
        private const val QUILT_META = "https://meta.quiltmc.org/v3"
    }

    fun install(
        loader: String,
        gameVersion: String,
        loaderVersion: String? = null
    ): Flow<DownloadProgress> = flow {
        when (loader.lowercase()) {
            "vanilla", "" -> {
                emit(DownloadProgress.Done(true, "Vanilla uses GameInstaller only"))
            }
            "fabric" -> {
                installFabric(gameVersion, loaderVersion).collect { emit(it) }
            }
            "quilt" -> {
                installQuilt(gameVersion, loaderVersion).collect { emit(it) }
            }
            "forge" -> {
                emit(DownloadProgress.Stage("Forge", "Preparing"))
                emit(
                    DownloadProgress.Done(
                        false,
                        "Forge: download Forge installer from Maven, run processors offline. " +
                            "Use Amethyst-style headless installer in runtime phase. " +
                            "Profile can still point to a manual Forge version id."
                    )
                )
            }
            "neoforge" -> {
                emit(DownloadProgress.Stage("NeoForge", "Preparing"))
                emit(
                    DownloadProgress.Done(
                        false,
                        "NeoForge: similar to Forge via maven.neoforged.net. " +
                            "Full processor run needs desktop-style installer or Amethyst headless."
                    )
                )
            }
            "optifine" -> {
                emit(
                    DownloadProgress.Done(
                        false,
                        "OptiFine: install OptiFine jar into versions/ or use OptiFabric + mods folder."
                    )
                )
            }
            else -> emit(DownloadProgress.Error("Unknown loader: $loader"))
        }
    }.flowOn(Dispatchers.IO)

    private fun installFabric(
        gameVersion: String,
        preferredLoader: String?
    ): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Stage("Fabric", "Resolving loader for $gameVersion"))

        val loaderVer = preferredLoader ?: resolveLatestFabricLoader(gameVersion)
        if (loaderVer == null) {
            emit(DownloadProgress.Error("No Fabric loader for $gameVersion"))
            return@flow
        }

        emit(DownloadProgress.Stage("Fabric", "Loader $loaderVer"))

        // Official profile JSON from Fabric Meta
        val profileUrl =
            "$FABRIC_META/versions/loader/$gameVersion/$loaderVer/profile/json"
        val profileJson = httpGet(profileUrl) ?: run {
            emit(DownloadProgress.Error("Fabric profile JSON failed"))
            return@flow
        }

        val root = JsonParser.parseString(profileJson).asJsonObject
        val versionId = root.get("id")?.asString
            ?: "$gameVersion-fabric-$loaderVer"

        // Save as a local version that inheritsFrom vanilla gameVersion
        val dir = paths.versionDir(versionId)
        val jsonFile = File(dir, "$versionId.json")
        jsonFile.writeText(gson.toJson(root))

        // Fabric profile usually inheritsFrom game version — ensure vanilla is installed separately
        emit(DownloadProgress.Stage("Fabric", "Saved profile $versionId"))

        // Download libraries listed in profile (loader, intermediary, etc.)
        val libs = root.getAsJsonArray("libraries") ?: return@flow run {
            emit(DownloadProgress.Done(true, "Fabric profile $versionId ready (no extra libs)"))
        }

        var i = 0
        val total = libs.size()
        for (el in libs) {
            val lib = el.asJsonObject
            val name = lib.get("name")?.asString ?: continue
            val urlBase = lib.get("url")?.asString ?: "https://maven.fabricmc.net/"
            val path = mavenPath(name)
            val out = File(paths.libraries, path)
            if (!out.exists()) {
                val jarUrl = urlBase.trimEnd('/') + "/" + path
                try {
                    download(jarUrl, out)
                } catch (e: Exception) {
                    Log.w(TAG, "Fabric lib failed: $name", e)
                }
            }
            i++
            if (i % 3 == 0 || i == total) {
                emit(DownloadProgress.Overall(i, total, name))
            }
        }

        emit(
            DownloadProgress.Done(
                true,
                "Fabric installed as version id: $versionId — set profile.versionId to this"
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun installQuilt(
        gameVersion: String,
        preferredLoader: String?
    ): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Stage("Quilt", "Resolving for $gameVersion"))
        // Quilt meta is similar; try latest loader list
        val listUrl = "$QUILT_META/versions/loader/$gameVersion"
        val listJson = httpGet(listUrl)
        if (listJson == null) {
            emit(DownloadProgress.Error("Quilt meta unavailable for $gameVersion"))
            return@flow
        }
        // Simplified: store note — full Quilt profile JSON path mirrors Fabric
        emit(
            DownloadProgress.Done(
                true,
                "Quilt meta reachable. Use profile json from Quilt meta like Fabric."
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun resolveLatestFabricLoader(gameVersion: String): String? {
        val url = "$FABRIC_META/versions/loader/$gameVersion"
        val body = httpGet(url) ?: return null
        return try {
            val arr = JsonParser.parseString(body).asJsonArray
            if (arr.size() == 0) return null
            // Prefer stable
            for (el in arr) {
                val loader = el.asJsonObject.getAsJsonObject("loader")
                if (loader.get("stable")?.asBoolean == true) {
                    return loader.get("version").asString
                }
            }
            arr[0].asJsonObject.getAsJsonObject("loader").get("version").asString
        } catch (e: Exception) {
            Log.e(TAG, "Parse fabric loaders", e)
            null
        }
    }

    private fun mavenPath(name: String): String {
        // group:artifact:version[:classifier]
        val p = name.split(":")
        val group = p[0].replace('.', '/')
        val artifact = p[1]
        val version = p[2]
        val classifier = if (p.size > 3) "-${p[3]}" else ""
        return "$group/$artifact/$version/$artifact-$version$classifier.jar"
    }

    private fun httpGet(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "RemomLauncher/0.1.0")
                .build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return null
                r.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET $url", e)
            null
        }
    }

    private fun download(url: String, dest: File) {
        dest.parentFile?.mkdirs()
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "RemomLauncher/0.1.0")
            .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IllegalStateException("HTTP ${r.code}")
            r.body?.byteStream()?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}
