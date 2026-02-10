package top.xiaojiang233.nekomusic

interface Platform {
    val name: String
    val isDebug: Boolean
}

expect fun getPlatform(): Platform