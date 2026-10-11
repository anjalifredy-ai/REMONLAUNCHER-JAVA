package com.remom.launcher.engine

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.remom.launcher.data.mojang.VersionJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads Minecraft assets (textures, sounds, lang files) from Mojang CDN.
 * Asset objects are content-addressed by SHA-1 under assets/objects/xx/hash
 */
class AssetDownloader(
    private val paths: GamePaths
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "AssetDownloader"
        private const val RESOURCES_BASE = "https://resources.download.minecraft.net"
        private const val PARALLEL = 8
    }

    data class AssetIndex(
        val objects: Map<String, AssetObject>
    )

    data class AssetObject(
        val hash: String,
        val size: Long
    )

    /**
     * Download asset index JSON + all object files referenced by [versionJson].
     * @param onProgress (completed, total, currentName)
     */
    suspend fun downloadAssets(
        versionJson: VersionJson,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val indexInfo = versionJson.assetIndex ?: run {
            Log.w(TAG, "No assetIndex in version JSON")
            return@withContext false
        }

        val indexesDir = File(paths.assets, "indexes").also { it.mkdirs() }
        val indexFile = File(indexesDir, "${indexInfo.id}.json")

        // Download index if missing
        if (!indexFile.exists() || indexFile.length() < 10) {
            try {
                downloadUrl(indexInfo.url, indexFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download asset index", e)
                return@withContext false
            }
        }

        val index: AssetIndex = try {
            val root = gson.fromJson(indexFile.readText(), JsonObject::class.java)
            val objectsEl = root.getAsJsonObject("objects")
            val map = mutableMapOf<String, AssetObject>()
            for ((name, el) in objectsEl.entrySet()) {
                val o = el.asJsonObject
                map[name] = AssetObject(
                    hash = o.get("hash").asString,
                    size = o.get("size").asLong
                )
            }
            AssetIndex(map)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse asset index", e)
            return@withContext false
        }

        val objects = index.objects.entries.toList()
        val total = objects.size
        var completed = 0
        val semaphore = Semaphore(PARALLEL)

        coroutineScope {
            objects.map { (name, obj) ->
                async {
                    semaphore.withPermit {
                        val ok = ensureObject(obj)
                        synchronized(this@AssetDownloader) {
                            completed++
                            onProgress(completed, total, name)
                        }
                        ok
                    }
                }
            }.awaitAll()
        }

        Log.i(TAG, "Assets done: $completed / $total")
        true
    }

    private fun ensureObject(obj: AssetObject): Boolean {
        val prefix = obj.hash.take(2)
        val out = File(paths.assets, "objects/$prefix/${obj.hash}")
        if (out.exists() && out.length() == obj.size) return true

        out.parentFile?.mkdirs()
        val url = "$RESOURCES_BASE/$prefix/${obj.hash}"
        return try {
            downloadUrl(url, out)
            out.exists() && out.length() == obj.size
        } catch (e: Exception) {
            Log.w(TAG, "Asset object failed: ${obj.hash}", e)
            false
        }
    }

    private fun downloadUrl(url: String, dest: File) {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RemomLauncher/0.1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} $url")
            }
            response.body?.byteStream()?.use { input ->
                FileOutputStream(tmp).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Empty body")
        }

        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
    }
}
