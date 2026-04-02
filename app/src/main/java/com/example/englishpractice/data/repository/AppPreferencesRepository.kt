package com.example.englishpractice.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.englishpractice.domain.model.CefrLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

class AppPreferencesRepository(private val context: Context) {
    val pilotLevelFlow: Flow<CefrLevel> = context.dataStore.data.map { preferences ->
        preferences[PILOT_LEVEL]
            ?.let { storedValue -> runCatching { CefrLevel.valueOf(storedValue) }.getOrNull() }
            ?: DEFAULT_PILOT_LEVEL
    }

    val speakingLocaleTagFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SPEAKING_LOCALE_TAG] ?: DEFAULT_SPEAKING_LOCALE_TAG
    }

    suspend fun setPilotLevel(level: CefrLevel) {
        context.dataStore.edit { preferences ->
            preferences[PILOT_LEVEL] = level.name
        }
    }

    suspend fun setSpeakingLocaleTag(localeTag: String) {
        context.dataStore.edit { preferences ->
            preferences[SPEAKING_LOCALE_TAG] = localeTag
        }
    }

    companion object {
        val DEFAULT_PILOT_LEVEL = CefrLevel.B2
        const val DEFAULT_SPEAKING_LOCALE_TAG = "en-US"

        private val PILOT_LEVEL = stringPreferencesKey("pilot_level")
        private val SPEAKING_LOCALE_TAG = stringPreferencesKey("speaking_locale_tag")
    }
}
