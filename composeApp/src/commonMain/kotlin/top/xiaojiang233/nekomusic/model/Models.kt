package top.xiaojiang233.nekomusic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BannerResponse(
    val banners: List<Banner>,
    val code: Int
)

@Serializable
data class Banner(
    val imageUrl: String? = null,
    val pic: String? = null,
    val targetId: Long,
    val targetType: Int,
    val titleColor: String?,
    val typeTitle: String?,
    val url: String? = null
)

@Serializable
data class PersonalizedPlaylistResponse(
    val result: List<Playlist>,
    val code: Int
)

@Serializable
data class DailyRecommendResponse(
    val recommend: List<Playlist>,
    val code: Int
)

@Serializable
data class Playlist(
    val id: Long,
    val type: Int? = null,
    val name: String,
    val copywriter: String? = null,
    val picUrl: String? = null,
    val coverImgUrl: String? = null, // Added for User Playlists
    val canDislike: Boolean? = null,
    val trackNumberUpdateTime: Long? = null,
    val playCount: Long? = null,
    val trackCount: Int = 0,
    val highQuality: Boolean? = null,
    val alg: String? = null,
    val creator: UserProfile? = null,
    val userId: Long? = null,
    val subscribed: Boolean? = null,
    val specialType: Int = 0 // 0: normal, 5: favorites
)
