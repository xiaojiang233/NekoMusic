package top.xiaojiang233.nekomusic.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.xiaojiang233.nekomusic.api.NeteaseApi

object FavoritesManager {
    private val _likedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedSongIds: StateFlow<Set<Long>> = _likedSongIds.asStateFlow()

    fun isLiked(id: Long): Boolean = _likedSongIds.value.contains(id)

    suspend fun loadLikes(uid: Long) {
         try {
             val res = NeteaseApi.getLikeList(uid)
             if (res.code == 200) {
                 _likedSongIds.value = res.ids.toSet()
             }
         } catch (e: Exception) {
             e.printStackTrace()
         }
    }

    suspend fun toggleLike(id: Long) {
        val current = _likedSongIds.value.contains(id)
        val newLikeState = !current
        // Optimistic update
        _likedSongIds.value = if (newLikeState) _likedSongIds.value + id else _likedSongIds.value - id

        try {
             val res = NeteaseApi.likeSong(id, newLikeState)
             if (res.code != 200) {
                 // Revert on failure
                 _likedSongIds.value = if (current) _likedSongIds.value + id else _likedSongIds.value - id
             }
        } catch (e: Exception) {
             // Revert
             _likedSongIds.value = if (current) _likedSongIds.value + id else _likedSongIds.value - id
        }
    }
}

