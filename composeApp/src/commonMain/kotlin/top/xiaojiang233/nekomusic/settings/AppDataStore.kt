package top.xiaojiang233.nekomusic.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

expect fun createDataStore(): DataStore<Preferences>

// File path helper - we can implement this in common if we use okio directly,
// or just expect the datastore creation which handles path internally.

