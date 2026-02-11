package top.xiaojiang233.nekomusic

interface Platform {
    val name: String
    val isDebug: Boolean
    val isAndroid: Boolean get() = name.startsWith("Android")
}

expect fun getPlatform(): Platform