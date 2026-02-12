package top.xiaojiang233.nekomusic.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable

expect fun showToast(message: String)

object ToastManager {
    // For Desktop UI
    private val _messages = MutableStateFlow<List<ToastMessage>>(emptyList())
    val messages: StateFlow<List<ToastMessage>> = _messages.asStateFlow()

    fun addMessage(msg: String) {
        val id = System.currentTimeMillis()
        val newMessage = ToastMessage(id, msg)
        _messages.value = _messages.value + newMessage

        // Auto remove
        MainScope().launch {
            delay(3000)
            removeMessage(id)
        }
    }

    fun removeMessage(id: Long) {
        _messages.value = _messages.value.filter { it.id != id }
    }
}

data class ToastMessage(val id: Long, val message: String)

