package top.xiaojiang233.nekomusic.ui.daily

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.daily_recommendations
import nekomusic.composeapp.generated.resources.back
import nekomusic.composeapp.generated.resources.error_format
import nekomusic.composeapp.generated.resources.retry
import nekomusic.composeapp.generated.resources.play_all
import org.jetbrains.compose.resources.stringResource
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.player.PlayerController
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
                title = { Text(stringResource(Res.string.daily_recommendations)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
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
                    Text(stringResource(Res.string.error_format, state.error!!))
                    Button(onClick = { viewModel.loadData() }) {
                        Text(stringResource(Res.string.retry))
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.daily_recommendations),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (state.songs.isNotEmpty()) {
                                    PlayerController.playList(state.songs)
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.play_all))
                        }
                    }
                }
                itemsIndexed(state.songs) { index, song ->
                    SongListItem(
                        index = index + 1,
                        song = song,
                        onClick = { PlayerController.playList(state.songs, index) }
                    )
                }
            }
        }
    }
}
