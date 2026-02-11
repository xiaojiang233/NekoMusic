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
    val trackCount: Int,
    val subscribed: Boolean? = null
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
    val ar: List<Artist> = emptyList(),
    val al: Album = Album(0, "", null),
    val dt: Long = 0 // Duration in ms
)

@Serializable
data class Artist(
    val id: Long,
    val name: String,
    val picUrl: String? = null,
    val img1v1Url: String? = null
)

@Serializable
data class Album(
    val id: Long,
    val name: String,
    val picUrl: String?,
    val publishTime: Long = 0,
    val size: Int = 0
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
    val songs: List<Song>? = null,
    val songCount: Int? = null,
    val playlists: List<Playlist>? = null,
    val playlistCount: Int? = null,
    val artists: List<Artist>? = null,
    val artistCount: Int? = null,
    val albums: List<Album>? = null,
    val albumCount: Int? = null
)

@Serializable
data class SearchResultResponse(
    val result: SearchResult,
    val code: Int
)

@Serializable
data class LyricResponse(
    val lrc: LyricData? = null,
    val yrc: LyricData? = null, // Verbatim lyrics (new)
    val tlyric: LyricData? = null, // Translation
    val romalrc: LyricData? = null, // Romaji/Romanized lyrics
    val code: Int
)

@Serializable
data class LyricData(
    val lyric: String
)

@Serializable
data class ArtistDetailResponse(
    val data: ArtistDetailData,
    val code: Int
)

@Serializable
data class ArtistDetailData(
    val artist: ArtistDetail,
    val user: ArtistUser? = null,
    val followed: Boolean? = null // Added top-level followed status check
)

@Serializable
data class ArtistUser(
    val followed: Boolean = false
)

@Serializable
data class ArtistDetail(
    val id: Long,
    val name: String,
    val cover: String? = null,
    val avatar: String? = null,
    val briefDesc: String? = null,
    val followed: Boolean? = false // Added followed status
)

@Serializable
data class ArtistAlbumResponse(
    val hotAlbums: List<Album>,
    val artist: Artist,
    val code: Int
)

@Serializable
data class AlbumDetailResponse(
    val songs: List<Song>,
    val album: Album,
    val code: Int
)

@Serializable
data class ArtistSublistResponse(
    val data: List<Artist>,
    val count: Int,
    val hasMore: Boolean,
    val code: Int
)

@Serializable
data class SubArtistResponse(
    val code: Int
)

@Serializable
data class ArtistTopSongsResponse(
    val songs: List<Song>,
    val code: Int
)

@Serializable
data class LikeResponse(
    val code: Int
)

@Serializable
data class LikeListResponse(
    val ids: List<Long>,
    val code: Int
)

@Serializable
data class SubscribePlaylistResponse(
    val code: Int
)

@Serializable
data class PlaylistTracksResponse(
    val code: Int
    // body depending on success might vary
)

@Serializable
data class CreatePlaylistResponse(
    val id: Long = 0,
    val playlist: Playlist? = null,
    val code: Int
)

@Serializable
data class DeletePlaylistResponse(
    val code: Int
)
