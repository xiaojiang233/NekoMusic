package top.xiaojiang233.nekomusic.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import top.xiaojiang233.nekomusic.NekoApp

actual fun createDataStore(): DataStore<Preferences> {
    val context = NekoApp.INSTANCE
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            context.filesDir.resolve("settings.preferences_pb").absolutePath.toPath()
        }
    )
}


