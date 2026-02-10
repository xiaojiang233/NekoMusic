package top.xiaojiang233.nekomusic.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

actual fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val file = File(System.getProperty("user.home"), ".nekomusic/settings.preferences_pb")
            file.parentFile.mkdirs()
            file.absolutePath.toPath()
        }
    )
}

