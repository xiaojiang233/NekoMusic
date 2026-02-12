package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.network.NetworkClient
import top.xiaojiang233.nekomusic.settings.SettingsManager
import kotlinx.coroutines.flow.first

data class DebugUiState(
    val url: String = "/search",
    val params: String = "keywords=周杰伦&limit=1",
    val response: String = "",
    val isLoading: Boolean = false
)

class DebugViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    fun updateUrl(newUrl: String) {
        _uiState.value = _uiState.value.copy(url = newUrl)
    }

    fun updateParams(newParams: String) {
        _uiState.value = _uiState.value.copy(params = newParams)
    }

    fun sendRequest() {
        val ui = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, response = "Loading...")
            try {
                val baseUrl = SettingsManager.getApiUrl().first()
                val cookie = SettingsManager.getCookie().first()

                // Parse params (k=v&k2=v2)
                val paramsMap = mutableMapOf<String, String>()
                ui.params.split("&").forEach { pair ->
                    val parts = pair.split("=")
                    if (parts.size == 2) {
                        paramsMap[parts[0].trim()] = parts[1].trim()
                    }
                }

                if (!cookie.isNullOrEmpty()) {
                    paramsMap["cookie"] = cookie
                }
                paramsMap["timestamp"] = kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()


                val result = NetworkClient.client.get("$baseUrl${ui.url}") {
                    paramsMap.forEach { (k, v) ->
                        parameter(k, v)
                    }
                }.body<String>()

                _uiState.value = _uiState.value.copy(isLoading = false, response = result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, response = "Error: ${e.message}")
            }
        }
    }
}

