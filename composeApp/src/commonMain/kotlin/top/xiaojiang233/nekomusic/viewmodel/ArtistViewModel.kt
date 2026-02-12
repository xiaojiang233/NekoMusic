package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.ArtistDetail
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.model.Album
import top.xiaojiang233.nekomusic.player.PlayerController

data class ArtistUiState(
    val artist: ArtistDetail? = null,
    val hotSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isSubscribed: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    // All Songs Pagination
    val allSongs: List<Song> = emptyList(),
    val allSongsOffset: Int = 0,
    val allSongsHasMore: Boolean = true,
    val isLoadingAllSongs: Boolean = false
)

class ArtistViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private var isLoaded = false // Flag to track initialization

    fun loadArtist(id: Long) {
        if (isLoaded && _uiState.value.artist?.id == id) return // Avoid reload if already loaded same artist

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Fetch in parallel ideally, but sequential is fine for now
                val detailResponse = NeteaseApi.getArtistDetail(id)
                val songsResponse = NeteaseApi.getArtistTopSongs(id)
                val albumsResponse = NeteaseApi.getArtistAlbums(id)

                if (detailResponse.code == 200 && songsResponse.code == 200 && albumsResponse.code == 200) {
                     val isSub = detailResponse.data.followed == true ||
                                 detailResponse.data.user?.followed == true ||
                                 detailResponse.data.artist.followed == true

                     println("ArtistViewModel: Loaded artist ${detailResponse.data.artist.name} (id=$id)")
                     println("  > data.followed: ${detailResponse.data.followed}")
                     println("  > data.user?.followed: ${detailResponse.data.user?.followed}")
                     println("  > data.artist.followed: ${detailResponse.data.artist.followed}")
                     println("  > resolved isSubscribed: $isSub")

                     // Fetch song details to get full info including album covers
                     val hotSongIds = songsResponse.songs.map { it.id }
                     val detailedHotSongs = if (hotSongIds.isNotEmpty()) {
                         NeteaseApi.getSongDetails(hotSongIds).songs
                     } else {
                         emptyList()
                     }

                     _uiState.value = _uiState.value.copy(
                         isLoading = false,
                         artist = detailResponse.data.artist,
                         hotSongs = detailedHotSongs,
                         albums = albumsResponse.hotAlbums,
                         isSubscribed = isSub
                     )
                     isLoaded = true
                } else {
                     _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load artist")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleFollow() {
        val artist = _uiState.value.artist ?: return
        val currentSub = _uiState.value.isSubscribed
        viewModelScope.launch {
             try {
                 val res = NeteaseApi.subArtist(artist.id, !currentSub)
                 if (res.code == 200) {
                     _uiState.value = _uiState.value.copy(isSubscribed = !currentSub)
                 }
             } catch (_: Exception) {
                 // handle error
             }
        }
    }

    fun playSong(song: Song) {
        val currentList = _uiState.value.hotSongs
        val index = currentList.indexOf(song).coerceAtLeast(0)
        PlayerController.playList(currentList, index)
    }

    fun playAllSong(song: Song) {
        val currentList = _uiState.value.allSongs
        val index = currentList.indexOf(song).coerceAtLeast(0)
        PlayerController.playList(currentList, index)
    }

    fun loadMoreAllSongs() {
        val ui = _uiState.value
        if (ui.artist == null || !ui.allSongsHasMore || ui.isLoadingAllSongs) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAllSongs = true)
            try {
                val res = NeteaseApi.getArtistSongs(
                    id = ui.artist.id,
                    limit = 50,
                    offset = ui.allSongsOffset
                )
                if (res.code == 200) {
                    val newSongs = res.songs
                    val newSongIds = newSongs.map { it.id }
                    val detailedNewSongs = if (newSongIds.isNotEmpty()) {
                        NeteaseApi.getSongDetails(newSongIds).songs
                    } else {
                        emptyList()
                    }
                    _uiState.value = _uiState.value.copy(
                        allSongs = ui.allSongs + detailedNewSongs,
                        allSongsOffset = ui.allSongsOffset + newSongs.size,
                        allSongsHasMore = newSongs.isNotEmpty() && (ui.allSongsOffset + newSongs.size < res.total),
                        isLoadingAllSongs = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingAllSongs = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoadingAllSongs = false)
            }
        }
    }
}
