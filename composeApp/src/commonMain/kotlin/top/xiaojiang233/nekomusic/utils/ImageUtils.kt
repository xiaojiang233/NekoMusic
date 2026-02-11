package top.xiaojiang233.nekomusic.utils

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap? {
    return try {
        // Strip data prefix if present
        val cleanBase64 = if (base64Str.contains(",")) {
            base64Str.substringAfter(",")
        } else {
            base64Str
        }

        val bytes = Base64.decode(cleanBase64)
        bytesToImageBitmap(bytes)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

expect fun bytesToImageBitmap(bytes: ByteArray): ImageBitmap?

fun String?.thumbnail(size: Int): String? {
    if (this == null) return null
    return if (this.contains("?")) {
        "$this&param=${size}y$size"
    } else {
        "$this?param=${size}y$size"
    }
}
