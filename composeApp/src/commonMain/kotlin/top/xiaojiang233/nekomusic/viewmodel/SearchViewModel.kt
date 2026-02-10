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
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.player.PlayerController
import top.xiaojiang233.nekomusic.settings.SettingsManager

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
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
            _uiState.value = _uiState.value.copy(results = emptyList())
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = NeteaseApi.search(newQuery, limit = 50)
                if (response.code == 200) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        results = response.result.songs ?: emptyList()
                    )
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
        onQueryChange(query)
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
        val currentList = _uiState.value.results
        val index = currentList.indexOf(song).coerceAtLeast(0)
        PlayerController.playList(currentList, index)
    }
}
