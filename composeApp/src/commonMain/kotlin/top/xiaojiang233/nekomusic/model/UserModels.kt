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
    val avatarUrl: String,
    val backgroundUrl: String?,
    val signature: String?
)

@Serializable
data class UserPlaylistResponse(
    val playlist: List<Playlist>,
    val code: Int
)

// Re-using Playlist from Models.kt, assuming fields align or we might need to make properties nullable there if they vary.
// Let's ensure Playlist in Models.kt is compatible.

