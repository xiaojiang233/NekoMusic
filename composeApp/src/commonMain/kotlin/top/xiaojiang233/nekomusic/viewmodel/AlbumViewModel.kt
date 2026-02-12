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

import top.xiaojiang233.nekomusic.model.Comment

data class AlbumUiState(
    val album: Album? = null,
    val tracks: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false
)

class AlbumViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun loadAlbum(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Launch loadComments in parallel or after?
                // Depending on UI requirements, we can just call it here or from UI
                loadComments(id)

                val response = NeteaseApi.getAlbum(id)
                if (response.code == 200) {
                     _uiState.value = _uiState.value.copy(
                         isLoading = false,
                         album = response.album,
                         tracks = response.songs.map {
                             // Inject album info unconditionally as these songs belong to this album
                             it.copy(al = response.album)
                         }
                     )
                } else {
                     _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load album")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun loadComments(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            try {
                // Fetch simple music comments for now as album comments structure is similar
                // NeteaseApi needs getAlbumComments if structure differs significantly,
                // but usually they share CommentResponse model
                val res = NeteaseApi.getAlbumComments(id)
                if (res.code == 200) {
                     _uiState.value = _uiState.value.copy(
                         comments = res.hotComments + res.comments,
                         isLoadingComments = false
                     )
                } else {
                     _uiState.value = _uiState.value.copy(isLoadingComments = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingComments = false)
            }
        }
    }

    fun playSong(song: Song) {
        val currentList = _uiState.value.tracks
        val index = currentList.indexOf(song).coerceAtLeast(0)
        PlayerController.playList(currentList, index)
    }

    fun playAll() {
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            PlayerController.playList(tracks, 0)
        }
    }
}
