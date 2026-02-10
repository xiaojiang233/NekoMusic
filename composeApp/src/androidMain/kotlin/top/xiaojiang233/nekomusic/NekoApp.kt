package top.xiaojiang233.nekomusic

import android.app.Application

class NekoApp : Application() {
    companion object {
        lateinit var INSTANCE: NekoApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }
}

