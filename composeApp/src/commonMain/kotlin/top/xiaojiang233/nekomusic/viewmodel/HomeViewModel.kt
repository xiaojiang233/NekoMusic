package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Banner
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.settings.SettingsManager

data class HomeUiState(
    val banners: List<Banner> = emptyList(),
    val recommendedPlaylists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showBanner: Boolean = true
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeSettings()
        observeLoginStatus()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            SettingsManager.showBanner().collect { show ->
                _uiState.value = _uiState.value.copy(showBanner = show)
            }
        }
    }

    private fun observeLoginStatus() {
        viewModelScope.launch {
            SettingsManager.getCookie().collect {
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Fetch in parallel if needed, for simplicity sequential here or use async
                val bannerResponse = NeteaseApi.getBanners()

                // Use /personalized endpoint (getRecommendedPlaylists) which supports limits and pagination.
                // It returns personalized results when logged in (cookie present).
                // Avoid /recommend/resource as it returns a small fixed number of playlists.
                val playlistResponse = NeteaseApi.getRecommendedPlaylists(limit = 30)

                _uiState.value = _uiState.value.copy(
                    banners = bannerResponse.banners,
                    recommendedPlaylists = playlistResponse.result,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
                e.printStackTrace()
            }
        }
    }
}
