package app.tidelio.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.tidelio.domain.entries.EntryLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.tidelioDataStore: DataStore<Preferences> by preferencesDataStore(name = "tidelio_prefs")

data class UserPrefs(
    val presets: List<Int>,
    val reducedMotion: Boolean,
    val setupDone: Boolean,
) {
    companion object {
        val DEFAULT_PRESETS = listOf(150, 250, 350, 500)
    }
}

class PreferencesRepository(context: Context) {

    private val store = context.applicationContext.tidelioDataStore

    private val presetKeys = (1..4).map { intPreferencesKey("preset_$it") }
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")
    private val setupDoneKey = booleanPreferencesKey("setup_done")

    val prefs: Flow<UserPrefs> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserPrefs(
                presets = presetKeys.mapIndexed { i, key ->
                    (p[key] ?: UserPrefs.DEFAULT_PRESETS[i]).coerceIn(EntryLimits.MIN_ML, EntryLimits.MAX_ML)
                },
                reducedMotion = p[reducedMotionKey] ?: false,
                setupDone = p[setupDoneKey] ?: false,
            )
        }

    suspend fun setPresets(values: List<Int>) {
        require(values.size == 4)
        store.edit { p -> values.forEachIndexed { i, v -> p[presetKeys[i]] = v } }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        store.edit { it[reducedMotionKey] = enabled }
    }

    suspend fun setSetupDone(done: Boolean) {
        store.edit { it[setupDoneKey] = done }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}
