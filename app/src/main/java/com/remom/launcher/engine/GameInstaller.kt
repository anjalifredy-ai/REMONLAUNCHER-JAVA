package com.remom.launcher.engine

import android.util.Log
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
 * Downloads and verifies everything needed to run a Minecraft version:
 * - version JSON
 * - client JAR
 * - libraries
 * - (assets index + objects can be expanded later)
 *
 * This is the launcher-side half of the engine.
 * Actual JVM + GL execution is handled by the runtime backend (Phase 3).
 */
class GameInstaller(
    private val paths: GamePaths,
    private val mojang: MojangRepository = MojangRepository()
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GameInstaller"
    }

    /**
     * Full prepare pipeline for a version id (e.g. "1.21.3").
     * Emits progress events.
     */
    fun prepare(versionId: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Stage("Resolving", versionId))

        // 1. Find version entry in manifest
        val manifestResult = mojang.getManifest()
        val manifest = manifestResult.getOrElse {
            emit(DownloadProgress.Error("Failed to load version manifest", it))
            return@flow
        }

        val entry = manifest.versions.find { it.id == versionId }
        if (entry == null) {
            emit(DownloadProgress.Error("Version $versionId not found in manifest"))
            return@flow
        }

        // 2. Download / cache version JSON
        emit(DownloadProgress.Stage("Version JSON", versionId))
        val versionJson = ensureVersionJson(entry)
        if (versionJson == null) {
            emit(DownloadProgress.Error("Could not download version JSON"))
            return@flow
        }

        // 3. Client JAR
        emit(DownloadProgress.Stage("Client JAR", versionId))
        val clientOk = ensureClientJar(versionId, versionJson) { downloaded, total ->
            // can't emit from nested easily without callback redesign; simplified
        }
        if (!clientOk) {
            emit(DownloadProgress.Error("Client JAR download failed"))
            return@flow
        }
        emit(DownloadProgress.Overall(1, 3, "Client JAR ready"))

        // 4. Libraries
        emit(DownloadProgress.Stage("Libraries", "Downloading required jars"))
        val libs = versionJson.libraries.orEmpty()
        var done = 0
        val total = libs.size.coerceAtLeast(1)
        for (lib in libs) {
            val ok = ensureLibrary(lib)
            done++
            emit(DownloadProgress.Overall(done, total, lib.name))
            if (!ok) {
                Log.w(TAG, "Library failed (may be OS-specific): ${lib.name}")
            }
        }

        // 5. Natives dir ready
        paths.nativesForVersion(versionId)

        emit(DownloadProgress.Done(true, "Version $versionId is ready to launch"))
    }.flowOn(Dispatchers.IO)

    private suspend fun ensureVersionJson(entry: VersionEntry): VersionJson? {
        val file = paths.versionJson(entry.id)
        if (file.exists() && file.length() > 100) {
            return try {
                val text = file.readText()
                // Re-fetch details via API for typed object
                mojang.getVersionDetails(entry.url).getOrNull() ?: run {
                    // fallback parse not strictly needed if API works
                    mojang.getVersionDetails(entry.url).getOrNull()
                }
            } catch (e: Exception) {
                null
            }
        }

        val details = mojang.getVersionDetails(entry.url).getOrNull() ?: return null
        // Also save raw for offline
        try {
            downloadToFile(entry.url, file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving version json", e)
        }
        return details
    }

    private fun ensureClientJar(
        versionId: String,
        versionJson: VersionJson,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Boolean {
        val jar = paths.versionJar(versionId)
        val info = versionJson.downloads?.client ?: return false

        if (jar.exists() && jar.length() == info.size && verifySha1(jar, info.sha1)) {
            return true
        }

        return try {
            downloadToFile(info.url, jar, onProgress)
            verifySha1(jar, info.sha1)
        } catch (e: Exception) {
            Log.e(TAG, "Client jar download failed", e)
            false
        }
    }

    private fun ensureLibrary(lib: Library): Boolean {
        val artifact = lib.downloads?.artifact ?: return true // no artifact = skip
        val pathParts = lib.name.split(":")
        if (pathParts.size < 3) return false

        // Maven path: group/artifact/version/artifact-version.jar
        val group = pathParts[0].replace('.', '/')
        val artifactId = pathParts[1]
        val version = pathParts[2]
        val fileName = "$artifactId-$version.jar"
        val relative = "$group/$artifactId/$version/$fileName"

        val out = File(paths.libraries, relative)
        out.parentFile?.mkdirs()

        if (out.exists() && out.length() == artifact.size && verifySha1(out, artifact.sha1)) {
            return true
        }

        // Skip rules that exclude our OS (we still try; rules mainly for natives)
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
