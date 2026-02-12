package top.xiaojiang233.nekomusic.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDetailResponse(
    val level: Int,
    val listenSongs: Int,
    val profile: UserProfile? = null,
    val code: Int
)

@Serializable
data class UserProfile(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null,
    val signature: String? = null,
    val follows: Int = 0,
    val followeds: Int = 0,
    val eventCount: Int = 0,
    val playlistCount: Int = 0,
    val playlistBeSubscribedCount: Int = 0
)

@Serializable
data class UserPlaylistResponse(
    val playlist: List<Playlist>,
    val code: Int
)

@Serializable
data class UserFollowsResponse(
    val follow: List<UserProfile>,
    val code: Int,
    val more: Boolean = false
)

@Serializable
data class UserFollowedsResponse(
    val followeds: List<UserProfile>,
    val code: Int,
    val more: Boolean = false
)

@Serializable
data class FollowUserResponse(
    val code: Int,
    val message: String? = null
)

@Serializable
data class ListenDataTotalResponse(
    val code: Int,
    val data: ListenDataTotal? = null,
    val message: String? = null
)

@Serializable
data class ListenDataTotal(
    val totalDuration: Long
)

// Re-using Playlist from Models.kt, assuming fields align or we might need to make properties nullable there if they vary.
// Let's ensure Playlist in Models.kt is compatible.
