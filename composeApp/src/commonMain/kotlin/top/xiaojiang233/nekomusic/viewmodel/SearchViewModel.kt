package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Artist
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.player.PlayerController
import top.xiaojiang233.nekomusic.settings.SettingsManager

data class SearchUiState(
    val query: String = "",
    val songResults: List<Song> = emptyList(),
    val playlistResults: List<Playlist> = emptyList(),
    val artistResults: List<Artist> = emptyList(),
    val searchType: Int = 1,
    val history: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            SettingsManager.getSearchHistory().collect { history ->
                _uiState.value = _uiState.value.copy(history = history)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(
                songResults = emptyList(),
                playlistResults = emptyList(),
                artistResults = emptyList()
            )
            return
        }

        performSearch(newQuery, _uiState.value.searchType)
    }

    fun onTabChange(type: Int) {
        if (_uiState.value.searchType == type) return
        _uiState.value = _uiState.value.copy(searchType = type)
        // Trigger search immediately if query exists
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query, type)
        }
    }

    private fun performSearch(query: String, type: Int) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = NeteaseApi.search(query, limit = 50, type = type)
                if (response.code == 200) {
                    when (type) {
                         1 -> _uiState.value = _uiState.value.copy(isLoading = false, songResults = response.result.songs ?: emptyList())
                         1000 -> _uiState.value = _uiState.value.copy(isLoading = false, playlistResults = response.result.playlists ?: emptyList())
                         100 -> _uiState.value = _uiState.value.copy(isLoading = false, artistResults = response.result.artists ?: emptyList())
                         else -> _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Search failed: ${response.code}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun onSearch(query: String) {
        // onQueryChange(query) // This calls performSearch
        _uiState.value = _uiState.value.copy(query = query)
        performSearch(query, _uiState.value.searchType)

        if (query.isNotBlank()) {
            viewModelScope.launch {
                SettingsManager.addSearchHistory(query)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            SettingsManager.clearSearchHistory()
        }
    }

    fun playSong(song: Song) {
        val currentList = _uiState.value.songResults
        val index = currentList.indexOf(song).coerceAtLeast(0)
        PlayerController.playList(currentList, index)
    }
}
