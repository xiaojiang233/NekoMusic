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
import top.xiaojiang233.nekomusic.player.PlayerController

data class ArtistUiState(
    val artist: ArtistDetail? = null,
    val hotSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ArtistViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    fun loadArtist(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Fetch in parallel ideally, but sequential is fine for now
                val detailResponse = NeteaseApi.getArtistDetail(id)
                val songsResponse = NeteaseApi.getArtistTopSongs(id)

                if (detailResponse.code == 200 && songsResponse.code == 200) {
                     _uiState.value = _uiState.value.copy(
                         isLoading = false,
                         artist = detailResponse.data.artist,
                         hotSongs = songsResponse.songs
                     )
                } else {
                     _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load artist")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun playSong(song: Song) {
        val currentList = _uiState.value.hotSongs
        val index = currentList.indexOf(song).coerceAtLeast(0)
        PlayerController.playList(currentList, index)
    }
}

