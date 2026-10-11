package com.remom.launcher.data.modrinth

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Modrinth API v2 client interfaces.
 * Docs: https://docs.modrinth.com/
 * Base URL: https://api.modrinth.com/v2/
 */
interface ModrinthApi {

    @GET("search")
    suspend fun search(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("index") index: String = "relevance",
        @Query("facets") facets: String? = null // e.g. [["project_type:mod"],["versions:1.21.1"]]
    ): ModrinthSearchResponse

    @GET("project/{id}")
    suspend fun getProject(@Path("id") id: String): ModrinthProject

    @GET("project/{id}/version")
    suspend fun getVersions(
        @Path("id") id: String,
        @Query("loaders") loaders: String? = null,
        @Query("game_versions") gameVersions: String? = null
    ): List<ModrinthVersion>

    @GET("version/{id}")
    suspend fun getVersion(@Path("id") id: String): ModrinthVersion
}

data class ModrinthSearchResponse(
    val hits: List<ModrinthHit>,
    val offset: Int,
    val limit: Int,
    val total_hits: Int
)

data class ModrinthHit(
    val project_id: String,
    val project_type: String,
    val slug: String,
    val author: String,
    val title: String,
    val description: String,
    val categories: List<String>,
    val display_categories: List<String>,
    val versions: List<String>,
    val downloads: Int,
    val follows: Int,
    val icon_url: String?,
    val date_created: String,
    val date_modified: String,
    val latest_version: String?,
    val license: String?,
    val client_side: String?,
    val server_side: String?,
    val gallery: List<String>?
)

data class ModrinthProject(
    val id: String,
    val slug: String,
    val project_type: String,
    val team: String,
    val title: String,
    val description: String,
    val body: String?,
    val published: String,
    val updated: String,
    val status: String,
    val license: License?,
    val client_side: String,
    val server_side: String,
    val downloads: Int,
    val followers: Int,
    val categories: List<String>,
    val additional_categories: List<String>?,
    val game_versions: List<String>?,
    val loaders: List<String>?,
    val versions: List<String>?,
    val icon_url: String?,
    val issues_url: String?,
    val source_url: String?,
    val wiki_url: String?,
    val discord_url: String?
)

data class License(
    val id: String,
    val name: String,
    val url: String?
)

data class ModrinthVersion(
    val id: String,
    val project_id: String,
    val author_id: String?,
    val featured: Boolean,
    val name: String,
    val version_number: String,
    val changelog: String?,
    val changelog_url: String?,
    val date_published: String,
    val downloads: Int,
    val version_type: String, // release, beta, alpha
    val status: String,
    val files: List<ModrinthFile>,
    val dependencies: List<ModrinthDependency>?,
    val game_versions: List<String>,
    val loaders: List<String>
)

data class ModrinthFile(
    val hashes: Map<String, String>,
    val url: String,
    val filename: String,
    val primary: Boolean,
    val size: Long,
    val file_type: String?
)

data class ModrinthDependency(
    val version_id: String?,
    val project_id: String?,
    val file_name: String?,
    val dependency_type: String // required, optional, incompatible, embedded
)
