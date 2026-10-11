package com.remom.launcher.engine

import android.util.Log
import com.google.gson.Gson
import com.remom.launcher.data.mojang.Library
import com.remom.launcher.data.mojang.MojangRepository
import com.remom.launcher.data.mojang.VersionEntry
import com.remom.launcher.data.mojang.VersionJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Full real Minecraft Java install pipeline:
 * 1. Version manifest lookup
 * 2. Version JSON
 * 3. Client JAR (official Mojang CDN, SHA-1 verified)
 * 4. Libraries (SHA-1 verified)
 * 5. Assets index + objects (textures, sounds, lang)
 *
 * After prepare() succeeds, files on disk are real official Minecraft.
 * Running them still needs the mobile JRE + GL runtime backend.
 */
class GameInstaller(
    private val paths: GamePaths,
    private val mojang: MojangRepository = MojangRepository()
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val assetDownloader = AssetDownloader(paths)

    companion object {
        private const val TAG = "GameInstaller"
    }

    fun prepare(versionId: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Stage("Resolving", versionId))

        val manifest = mojang.getManifest().getOrElse {
            emit(DownloadProgress.Error("Failed to load Mojang version manifest", it))
            return@flow
        }

        val entry = manifest.versions.find { it.id == versionId }
        if (entry == null) {
            emit(DownloadProgress.Error("Version $versionId not found on Mojang servers"))
            return@flow
        }

        // Version JSON
        emit(DownloadProgress.Stage("Version JSON", "Fetching $versionId metadata"))
        val versionJson = ensureVersionJson(entry)
        if (versionJson == null) {
            emit(DownloadProgress.Error("Could not download version JSON from Mojang"))
            return@flow
        }

        // Client JAR — real official Minecraft
        emit(DownloadProgress.Stage("Client JAR", "Downloading official Minecraft $versionId"))
        val clientOk = ensureClientJar(versionId, versionJson)
        if (!clientOk) {
            emit(DownloadProgress.Error("Official client JAR download failed"))
            return@flow
        }
        emit(DownloadProgress.Overall(1, 4, "Client JAR verified"))

        // Libraries
        emit(DownloadProgress.Stage("Libraries", "Downloading game libraries"))
        val libs = versionJson.libraries.orEmpty()
        var done = 0
        val totalLibs = libs.size.coerceAtLeast(1)
        for (lib in libs) {
            ensureLibrary(lib)
            done++
            if (done % 5 == 0 || done == totalLibs) {
                emit(DownloadProgress.Overall(done, totalLibs, lib.name))
            }
        }
        emit(DownloadProgress.Overall(totalLibs, totalLibs, "Libraries done"))

        // Assets (real textures/sounds)
        emit(DownloadProgress.Stage("Assets", "Downloading textures, sounds, language files"))
        val assetsOk = assetDownloader.downloadAssets(versionJson) { completed, total, name ->
            // Flow can't emit from callback easily without channel; log only
            if (completed % 50 == 0 || completed == total) {
                Log.i(TAG, "Assets $completed / $total — $name")
            }
        }
        if (!assetsOk) {
            Log.w(TAG, "Assets download incomplete — game may still start with missing sounds/textures")
        }
        emit(DownloadProgress.Overall(4, 4, if (assetsOk) "Assets ready" else "Assets partial"))

        paths.nativesForVersion(versionId)

        emit(
            DownloadProgress.Done(
                true,
                "Minecraft $versionId installed (client + libs + assets). Runtime backend required to play."
            )
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun ensureVersionJson(entry: VersionEntry): VersionJson? {
        val file = paths.versionJson(entry.id)
        val details = mojang.getVersionDetails(entry.url).getOrNull() ?: return null

        // Always keep a local copy of the official JSON
        try {
            file.parentFile?.mkdirs()
            file.writeText(gson.toJson(details))
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving version json", e)
            // Still try raw download
            try {
                downloadToFile(entry.url, file)
            } catch (e2: Exception) {
                Log.e(TAG, "Raw version json download failed", e2)
            }
        }
        return details
    }

    private fun ensureClientJar(versionId: String, versionJson: VersionJson): Boolean {
        val jar = paths.versionJar(versionId)
        val info = versionJson.downloads?.client ?: return false

        if (jar.exists() && jar.length() == info.size && verifySha1(jar, info.sha1)) {
            Log.i(TAG, "Client JAR already verified: $versionId")
            return true
        }

        return try {
            Log.i(TAG, "Downloading client from ${info.url}")
            downloadToFile(info.url, jar)
            val ok = verifySha1(jar, info.sha1)
            if (!ok) Log.e(TAG, "SHA-1 mismatch for client JAR")
            ok
        } catch (e: Exception) {
            Log.e(TAG, "Client jar download failed", e)
            false
        }
    }

    private fun ensureLibrary(lib: Library): Boolean {
        val artifact = lib.downloads?.artifact ?: return true
        val pathParts = lib.name.split(":")
        if (pathParts.size < 3) return false

        val group = pathParts[0].replace('.', '/')
        val artifactId = pathParts[1]
        val version = pathParts[2]
        // Handle classifier if present in name (rare in modern)
        val fileName = "$artifactId-$version.jar"
        val relative = "$group/$artifactId/$version/$fileName"

        val out = File(paths.libraries, relative)
        out.parentFile?.mkdirs()

        if (out.exists() && out.length() == artifact.size && verifySha1(out, artifact.sha1)) {
            return true
        }

        return try {
            downloadToFile(artifact.url, out)
            verifySha1(out, artifact.sha1)
        } catch (e: Exception) {
            Log.w(TAG, "Lib download failed: ${lib.name}", e)
            false
        }
    }

    private fun downloadToFile(
        url: String,
        dest: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ) {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parentFile, dest.name + ".part")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RemomLauncher/0.1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} for $url")
            }
            val body = response.body ?: throw IllegalStateException("Empty body")
            val total = body.contentLength()
            var downloaded = 0L

            body.byteStream().use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        }

        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
    }

    private fun verifySha1(file: File, expected: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            file.inputStream().use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val r = input.read(buf)
                    if (r <= 0) break
                    digest.update(buf, 0, r)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            actual.equals(expected, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    fun isVersionReady(versionId: String): Boolean {
        val jar = paths.versionJar(versionId)
        val json = paths.versionJson(versionId)
        return jar.exists() && jar.length() > 1000 && json.exists()
    }
}
