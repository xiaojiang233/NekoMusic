package top.xiaojiang233.nekomusic.utils

actual fun showToast(message: String) {
    ToastManager.addMessage(message)
}

