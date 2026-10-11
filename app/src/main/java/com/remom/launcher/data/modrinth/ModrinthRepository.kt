package com.remom.launcher.data.modrinth

import android.util.Log
import com.remom.launcher.data.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class ModrinthRepository {

    private val api = NetworkModule.modrinthApi
    private val downloadClient = OkHttpClient()

    suspend fun search(
        query: String,
        limit: Int = 20,
        projectType: String? = "mod" // mod, resourcepack, shader, datapack
    ): Result<ModrinthSearchResponse> = withContext(Dispatchers.IO) {
        try {
            val facets = if (projectType != null) {
                "[[\"project_type:$projectType\"]]"
            } else null
            val response = api.search(query = query, limit = limit, facets = facets)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("Modrinth", "Search failed", e)
            Result.failure(e)
        }
    }

    suspend fun getVersions(
        projectId: String,
        gameVersion: String? = null,
        loader: String? = null
    ): Result<List<ModrinthVersion>> = withContext(Dispatchers.IO) {
        try {
            val loaders = loader?.let { "[\"$it\"]" }
            val versions = gameVersion?.let { "[\"$it\"]" }
            val list = api.getVersions(projectId, loaders = loaders, gameVersions = versions)
            Result.success(list)
        } catch (e: Exception) {
            Log.e("Modrinth", "Get versions failed", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the primary file of a version directly into the target directory.
     * Returns the downloaded File on success.
     */
    suspend fun downloadVersion(
        version: ModrinthVersion,
        targetDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileMeta = version.files.firstOrNull { it.primary }
                ?: version.files.firstOrNull()
                ?: return@withContext Result.failure(Exception("No files in version"))

            targetDir.mkdirs()
            val outFile = File(targetDir, fileMeta.filename)

            val request = Request.Builder()
                .url(fileMeta.url)
                .header("User-Agent", "RemomLauncher/0.1.0")
                .build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(Exception("Empty body"))
            }

            Result.success(outFile)
        } catch (e: Exception) {
            Log.e("Modrinth", "Download failed", e)
            Result.failure(e)
        }
    }

    fun listInstalledMods(modsDir: File): List<File> {
        if (!modsDir.exists()) return emptyList()
        return modsDir.listFiles { f -> f.isFile && f.extension.equals("jar", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    fun deleteMod(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}
