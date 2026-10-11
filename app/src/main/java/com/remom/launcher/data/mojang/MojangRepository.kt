package com.remom.launcher.data.mojang

import android.util.Log
import com.remom.launcher.data.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MojangRepository {

    private val api = NetworkModule.mojangApi

    private var cachedManifest: VersionManifest? = null

    suspend fun getManifest(forceRefresh: Boolean = false): Result<VersionManifest> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedManifest != null) {
            return@withContext Result.success(cachedManifest!!)
        }
        try {
            val manifest = api.getVersionManifest()
            cachedManifest = manifest
            Result.success(manifest)
        } catch (e: Exception) {
            Log.e("Mojang", "Failed to fetch version manifest", e)
            Result.failure(e)
        }
    }

    suspend fun getReleaseVersions(): Result<List<VersionEntry>> {
        return getManifest().map { manifest ->
            manifest.versions.filter { it.type == "release" }
        }
    }

    suspend fun getAllVersions(
        includeSnapshots: Boolean = true,
        includeOld: Boolean = false
    ): Result<List<VersionEntry>> {
        return getManifest().map { manifest ->
            manifest.versions.filter { entry ->
                when (entry.type) {
                    "release" -> true
                    "snapshot" -> includeSnapshots
                    "old_beta", "old_alpha" -> includeOld
                    else -> false
                }
            }
        }
    }

    suspend fun getVersionDetails(url: String): Result<VersionJson> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.getVersionJson(url))
        } catch (e: Exception) {
            Log.e("Mojang", "Failed to fetch version json", e)
            Result.failure(e)
        }
    }

    fun getLatestRelease(): String? = cachedManifest?.latest?.release
    fun getLatestSnapshot(): String? = cachedManifest?.latest?.snapshot
}
