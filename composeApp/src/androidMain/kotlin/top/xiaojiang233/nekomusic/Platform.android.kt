package top.xiaojiang233.nekomusic

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val isDebug: Boolean = top.xiaojiang233.nekomusic.BuildConfig.DEBUG
}

actual fun getPlatform(): Platform = AndroidPlatform()