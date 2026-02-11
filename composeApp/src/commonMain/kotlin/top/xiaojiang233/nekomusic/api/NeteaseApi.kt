package top.xiaojiang233.nekomusic.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import top.xiaojiang233.nekomusic.model.*
import top.xiaojiang233.nekomusic.network.NetworkClient

import top.xiaojiang233.nekomusic.settings.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

object NeteaseApi {

    private suspend fun getBaseUrl(): String {
        return SettingsManager.getApiUrl().first()
    }

    private suspend fun getCookie(): String? {
        return SettingsManager.getCookie().first()
    }

    // Helper to make requests with cookie and base url
    private suspend inline fun <reified T> request(path: String, params: Map<String, Any?> = emptyMap()): T {
        val baseUrl = getBaseUrl()
        val cookie = getCookie()
        return NetworkClient.client.get("$baseUrl$path") {
            params.forEach { (k, v) ->
                if (v != null) parameter(k, v)
            }
            // Add timestamp to prevent caching for most requests, or at least for login ones
            parameter("timestamp", Clock.System.now().toEpochMilliseconds())

            if (!cookie.isNullOrEmpty()) {
               // Ensure os=pc is present for high quality audio access (as per new API requirement)
               val effectiveCookie = if (cookie.contains("os=pc")) cookie else "$cookie; os=pc"
               parameter("cookie", effectiveCookie)
            }
        }.body()
    }

    suspend fun getBanners(): BannerResponse {
        return request("/banner", mapOf("type" to 2))
    }

    suspend fun getRecommendedPlaylists(limit: Int = 10): PersonalizedPlaylistResponse {
        return request("/personalized", mapOf("limit" to limit))
    }

    suspend fun getDailyRecommendedPlaylists(): DailyRecommendResponse {
        return request("/recommend/resource")
    }

    // Login APIs
    suspend fun getQrKey(): LoginQrKeyResponse {
        return request("/login/qr/key")
    }

    suspend fun createQr(key: String): LoginQrCreateResponse {
        return request("/login/qr/create", mapOf("key" to key, "qrimg" to true))
    }

    suspend fun checkQr(key: String): LoginQrCheckResponse {
        return request("/login/qr/check", mapOf("key" to key))
    }

    suspend fun getLoginStatus(): LoginStatusResponse {
        return request("/login/status")
    }

    suspend fun getUserDetail(uid: Long): UserDetailResponse {
        return request("/user/detail", mapOf("uid" to uid))
    }

    suspend fun getUserPlaylist(uid: Long, limit: Int = 30, offset: Int = 0): UserPlaylistResponse {
        return request("/user/playlist", mapOf("uid" to uid, "limit" to limit, "offset" to offset))
    }

    suspend fun getUserFollows(uid: Long, limit: Int = 30, offset: Int = 0): UserFollowsResponse {
        return request("/user/follows", mapOf("uid" to uid, "limit" to limit, "offset" to offset))
    }

    suspend fun getUserFolloweds(uid: Long, limit: Int = 30, offset: Int = 0): UserFollowedsResponse {
        return request("/user/followeds", mapOf("uid" to uid, "limit" to limit, "offset" to offset))
    }

    suspend fun getPlaylistDetail(id: Long): PlaylistDetailResponse {
        return request("/playlist/detail", mapOf("id" to id))
    }

    suspend fun getSongDetails(ids: List<Long>): SongDetailResponse {
        return request("/song/detail", mapOf("ids" to ids.joinToString(",")))
    }

    suspend fun getDailySongs(): DailySongsResponse {
        return request("/recommend/songs")
    }

    suspend fun getSongUrl(id: Long, level: String = "standard"): SongUrlResponse {
        return request("/song/url/v1", mapOf("id" to id, "level" to level))
    }

    suspend fun search(keywords: String, limit: Int = 30, offset: Int = 0, type: Int = 1): CloudSearchResponse {
        return request("/cloudsearch", mapOf("keywords" to keywords, "limit" to limit, "offset" to offset, "type" to type))
    }

    suspend fun getLyrics(id: Long): LyricResponse {
        return request("/lyric/new", mapOf("id" to id))
    }

    suspend fun getArtistDetail(id: Long): ArtistDetailResponse {
        return request("/artist/detail", mapOf("id" to id))
    }

    suspend fun getArtistAlbums(id: Long, limit: Int = 30, offset: Int = 0): ArtistAlbumResponse {
        return request("/artist/album", mapOf("id" to id, "limit" to limit, "offset" to offset))
    }

    suspend fun getArtistSublist(limit: Int = 30, offset: Int = 0): ArtistSublistResponse {
        return request("/artist/sublist", mapOf("limit" to limit, "offset" to offset))
    }

    suspend fun subArtist(id: Long, sub: Boolean = true): SubArtistResponse {
        val t = if (sub) 1 else 0
        return request("/artist/sub", mapOf("id" to id, "t" to t))
    }

    suspend fun getAlbum(id: Long): AlbumDetailResponse {
        return request("/album", mapOf("id" to id))
    }

    suspend fun getArtistTopSongs(id: Long): ArtistTopSongsResponse {
        return request("/artist/top/song", mapOf("id" to id))
    }

    suspend fun likeSong(id: Long, like: Boolean = true): LikeResponse {
        return request("/like", mapOf("id" to id, "like" to like))
    }

    suspend fun getLikeList(uid: Long): LikeListResponse {
        return request("/likelist", mapOf("uid" to uid))
    }

    suspend fun subscribePlaylist(id: Long, subscribe: Boolean = true): SubscribePlaylistResponse {
        val t = if (subscribe) 1 else 2
        return request("/playlist/subscribe", mapOf("id" to id, "t" to t))
    }

    suspend fun addSongToPlaylist(op: String, pid: Long, trackIds: List<Long>): PlaylistTracksResponse {
        return request("/playlist/tracks", mapOf("op" to op, "pid" to pid, "tracks" to trackIds.joinToString(",")))
    }

    suspend fun createPlaylist(name: String, privacy: Int = 0): CreatePlaylistResponse {
        // privacy: 0 for public, 10 for private
        return request("/playlist/create", mapOf("name" to name, "privacy" to privacy))
    }

    suspend fun deletePlaylist(pid: Long): DeletePlaylistResponse {
        return request("/playlist/delete", mapOf("id" to pid))
    }

    suspend fun getMusicComments(id: Long, limit: Int = 20, offset: Int = 0): CommentResponse {
        return request("/comment/music", mapOf("id" to id, "limit" to limit, "offset" to offset))
    }
}
