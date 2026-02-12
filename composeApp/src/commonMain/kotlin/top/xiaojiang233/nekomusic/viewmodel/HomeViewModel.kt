package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitAll
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Banner
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.model.PrivateRoamingItem
import top.xiaojiang233.nekomusic.settings.SettingsManager
import top.xiaojiang233.nekomusic.utils.FavoritesManager
import top.xiaojiang233.nekomusic.player.PlayerController

data class HomeUiState(
    val banners: List<Banner> = emptyList(),
    val recommendedPlaylists: List<Playlist> = emptyList(),
    val privateRoaming: List<PrivateRoamingItem> = emptyList(),
    val otherRadarPlaylists: List<Playlist> = emptyList(),
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
                // Load favorites if logged in
                try {
                    val status = NeteaseApi.getLoginStatus()
                    status.data.profile?.userId?.let { uid ->
                        FavoritesManager.loadLikes(uid)
                    }
                } catch (e: Exception) {
                    // Ignore login status check failures (e.g. offline)
                }

                // Fetch in parallel if needed, for simplicity sequential here or use async
                val bannerResponse = NeteaseApi.getBanners()

                // Use /personalized endpoint (getRecommendedPlaylists) which supports limits and pagination.
                // It returns personalized results when logged in (cookie present).
                // Avoid /recommend/resource as it returns a small fixed number of playlists.
                val playlistResponse = NeteaseApi.getRecommendedPlaylists(limit = 50)

                // Fetch Private Roaming
                val roamingResponse = try {
                     NeteaseApi.getPrivateRoamingList()
                } catch (e: Exception) {
                     null // Optional
                }

                // Fetch Other Radar Playlists
                val radarIds = listOf(3136952023L, 2829896389L, 2829816518L, 2829883282L, 2829920189L)
                val radarPlaylists = radarIds.map { id ->
                    async {
                        try {
                            val detail = NeteaseApi.getPlaylistDetail(id).playlist
                            Playlist(
                                id = detail.id,
                                name = detail.name,
                                picUrl = detail.coverImgUrl,
                                coverImgUrl = detail.coverImgUrl,
                                trackCount = detail.trackCount,
                                creator = detail.creator
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                _uiState.value = _uiState.value.copy(
                    banners = bannerResponse.banners,
                    recommendedPlaylists = playlistResponse.result,
                    privateRoaming = roamingResponse?.result ?: emptyList(),
                    otherRadarPlaylists = radarPlaylists,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Unknown Error", isLoading = false)
            }
        }
    }

    fun openLikedSongsPlaylist(onPlaylistFound: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val status = NeteaseApi.getLoginStatus()
                val uid = status.data.profile?.userId ?: return@launch

                val playlists = NeteaseApi.getUserPlaylist(uid)
                if (playlists.playlist.isNotEmpty()) {
                    // Usually the first playlist is the "Liked Songs"
                    val likedPlaylistId = playlists.playlist[0].id
                    onPlaylistFound(likedPlaylistId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
