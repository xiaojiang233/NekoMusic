package top.xiaojiang233.nekomusic.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDetailResponse(
    val playlist: PlaylistDetail,
    val code: Int
)

@Serializable
data class PlaylistDetail(
    val id: Long,
    val name: String,
    val coverImgUrl: String?,
    val description: String?,
    val trackIds: List<TrackId>,
    val creator: UserProfile? = null,
    val tags: List<String>? = null,
    val trackCount: Int
)

@Serializable
data class TrackId(
    val id: Long,
    val v: Int? = null,
    val at: Long? = null
)

@Serializable
data class SongDetailResponse(
    val songs: List<Song>,
    val code: Int
)

@Serializable
data class Song(
    val id: Long,
    val name: String,
    val ar: List<Artist>,
    val al: Album,
    val dt: Long // Duration in ms
)

@Serializable
data class Artist(
    val id: Long,
    val name: String
)

@Serializable
data class Album(
    val id: Long,
    val name: String,
    val picUrl: String?
)

@Serializable
data class DailySongsResponse(
    val data: DailySongsData,
    val code: Int
)

@Serializable
data class DailySongsData(
    val dailySongs: List<Song>
)

@Serializable
data class SongUrlResponse(
    val data: List<SongUrl>,
    val code: Int
)

@Serializable
data class SongUrl(
    val id: Long,
    val url: String?
)

@Serializable
data class CloudSearchResponse(
    val result: SearchResult,
    val code: Int
)

@Serializable
data class SearchResult(
    val songs: List<Song>?,
    val songCount: Int
)

@Serializable
data class LyricResponse(
    val lrc: LyricData? = null,
    val yrc: LyricData? = null, // Verbatim lyrics (new)
    val tlyric: LyricData? = null, // Translation
    val code: Int
)

@Serializable
data class LyricData(
    val lyric: String
)
