package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Album
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.player.PlayerController

data class AlbumUiState(
    val album: Album? = null,
    val tracks: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AlbumViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun loadAlbum(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = NeteaseApi.getAlbum(id)
                if (response.code == 200) {
                     _uiState.value = _uiState.value.copy(
                         isLoading = false,
                         album = response.album,
                         tracks = response.songs
                     )
                } else {
                     _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load album")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun playSong(song: Song) {
        val currentList = _uiState.value.tracks
        val index = currentList.indexOf(song).coerceAtLeast(0)
        PlayerController.playList(currentList, index)
    }
}

