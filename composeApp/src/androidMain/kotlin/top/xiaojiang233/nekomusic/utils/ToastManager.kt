package top.xiaojiang233.nekomusic.utils

import android.widget.Toast
import top.xiaojiang233.nekomusic.NekoApp

actual fun showToast(message: String) {
    // Requires Application Context
    try {
        val context = NekoApp.INSTANCE
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

