package com.remom.launcher.data.mojang

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Mojang version manifest endpoints.
 * Manifest: https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
 */
interface MojangApi {

    @GET("mc/game/version_manifest_v2.json")
    suspend fun getVersionManifest(): VersionManifest

    @GET
    suspend fun getVersionJson(@Url url: String): VersionJson
}

data class VersionManifest(
    val latest: LatestVersions,
    val versions: List<VersionEntry>
)

data class LatestVersions(
    val release: String,
    val snapshot: String
)

data class VersionEntry(
    val id: String,
    val type: String, // release, snapshot, old_beta, old_alpha
    val url: String,
    val time: String,
    val releaseTime: String,
    val sha1: String? = null
)

data class VersionJson(
    val id: String,
    val type: String,
    val mainClass: String? = null,
    val libraries: List<Library>? = null,
    val downloads: Downloads? = null,
    val assetIndex: AssetIndex? = null,
    val javaVersion: JavaVersionInfo? = null,
    val arguments: Arguments? = null,
    val minecraftArguments: String? = null
)

data class Downloads(
    val client: DownloadInfo? = null,
    val server: DownloadInfo? = null
)

data class DownloadInfo(
    val sha1: String,
    val size: Long,
    val url: String
)

data class AssetIndex(
    val id: String,
    val sha1: String,
    val size: Long,
    val totalSize: Long? = null,
    val url: String
)

data class JavaVersionInfo(
    val component: String,
    val majorVersion: Int
)

data class Library(
    val name: String,
    val downloads: LibraryDownloads? = null,
    val rules: List<Rule>? = null
)

data class LibraryDownloads(
    val artifact: DownloadInfo? = null
)

data class Rule(
    val action: String,
    val os: OsRule? = null
)

data class OsRule(
    val name: String? = null
)

data class Arguments(
    val game: List<Any>? = null,
    val jvm: List<Any>? = null
)
