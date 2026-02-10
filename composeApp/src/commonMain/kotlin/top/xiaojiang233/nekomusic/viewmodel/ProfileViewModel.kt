package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.model.UserDetailResponse

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userId: Long = 0,
    val userDetail: UserDetailResponse? = null,
    val userPlaylists: List<Playlist> = emptyList(),
    val error: String? = null
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 1. Get Login Status to find UID
                println("ProfileViewModel: Fetching login status...")
                val status = NeteaseApi.getLoginStatus()
                val profile = status.data.profile

                if (profile != null) {
                    val uid = profile.userId
                    println("ProfileViewModel: Logged in as $uid")

                    // 2. Fetch User Detail (Level, etc.)
                   try {
                       val detail = NeteaseApi.getUserDetail(uid)
                       // 3. Fetch User Playlists
                       val playlists = NeteaseApi.getUserPlaylist(uid)

                       _uiState.value = _uiState.value.copy(
                           isLoading = false,
                           isLoggedIn = true,
                           userId = uid,
                           userDetail = detail,
                           userPlaylists = playlists.playlist
                       )
                   } catch (apiEx: Exception) {
                        apiEx.printStackTrace()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userId = uid,
                            error = "Failed to load details: ${apiEx.message}"
                        )
                   }

                } else {
                    println("ProfileViewModel: Not logged in")
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

