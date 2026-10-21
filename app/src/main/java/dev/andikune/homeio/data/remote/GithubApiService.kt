package dev.andikune.homeio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GithubAsset> = emptyList()
)

@Serializable
data class GithubAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url") val downloadUrl: String
)

interface GithubApiService {
    @GET("repos/andikune-ux/HOME.iO-TV/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
}
