package top.xiaojiang233.nekomusic.ui.daily

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.player.AudioManager
import top.xiaojiang233.nekomusic.ui.playlist.SongListItem

class DailySongsViewModel : ViewModel() {
    var uiState by mutableStateOf(DailySongsUiState())
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                val response = NeteaseApi.getDailySongs()
                if (response.code == 200) {
                    uiState = uiState.copy(
                        isLoading = false,
                        songs = response.data.dailySongs
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Failed to load: ${response.code}"
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            try {
                val response = NeteaseApi.getSongUrl(song.id)
                if (response.code == 200 && response.data.isNotEmpty()) {
                    val url = response.data[0].url
                    if (url != null) {
                        AudioManager.play(url, song)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class DailySongsUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySongsScreen(
    onBackClick: () -> Unit,
    viewModel: DailySongsViewModel = viewModel { DailySongsViewModel() }
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Recommendations") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${state.error}")
                    Button(onClick = { viewModel.loadData() }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                itemsIndexed(state.songs) { index, song ->
                    SongListItem(index + 1, song, onClick = { viewModel.playSong(song) })
                }
            }
        }
    }
}
