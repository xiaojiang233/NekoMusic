package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.settings.SettingsManager

data class LoginUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isLoggedIn: Boolean = false,
    val accountName: String? = null,
    val avatarUrl: String? = null
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
    }

    fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                // If we have a cookie, we can assume we might be logged in, or verify
                // But let's check profile
                val status = NeteaseApi.getLoginStatus()
                // code 200 usually means logged in if generic response
                // But getLoginStatus returns generic wrapper. data.code 200 = logged in?
                // Actually LoginStatusResponse wraps LoginStatusData.
                if (status.data.profile != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        accountName = status.data.profile.nickname,
                        avatarUrl = status.data.profile.avatarUrl
                    )
                } else {
                     _uiState.value = _uiState.value.copy(isLoggedIn = false)
                }
            } catch (e: Exception) {
                // Ignore or show error
                e.printStackTrace()
            }
        }
    }

    fun loginWithCookie(cookie: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = "Logging in...")
            SettingsManager.setCookie(cookie.trim())
            checkLoginStatus()
            _uiState.value = _uiState.value.copy(isLoading = false, message = "Cookie Saved")
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Netease logout API /logout usually
            SettingsManager.setCookie("")
            _uiState.value = LoginUiState(isLoggedIn = false)
        }
    }

    fun setLyricsFontSize(size: Float) {
        viewModelScope.launch {
            SettingsManager.setLyricsFontSize(size)
        }
    }

    fun setLyricsBlurIntensity(intensity: Float) {
        viewModelScope.launch {
            SettingsManager.setLyricsBlurIntensity(intensity)
        }
    }

    fun setLyricsFontFamily(family: String) {
        viewModelScope.launch {
            SettingsManager.setLyricsFontFamily(family)
        }
    }
}
