package top.xiaojiang233.nekomusic

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform