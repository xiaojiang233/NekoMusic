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
    val error: String? = null
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

                     _uiState.value = _uiState.value.copy(
                         isLoading = false,
                         artist = detailResponse.data.artist,
                         hotSongs = songsResponse.songs,
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
}
