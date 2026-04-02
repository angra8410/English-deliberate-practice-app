package com.example.englishpractice.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

class AppPreferencesRepository(private val context: Context) {
    val speakingLocaleTagFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SPEAKING_LOCALE_TAG] ?: DEFAULT_SPEAKING_LOCALE_TAG
    }

    suspend fun setSpeakingLocaleTag(localeTag: String) {
        context.dataStore.edit { preferences ->
            preferences[SPEAKING_LOCALE_TAG] = localeTag
        }
    }

    companion object {
        const val DEFAULT_SPEAKING_LOCALE_TAG = "en-US"

        private val SPEAKING_LOCALE_TAG = stringPreferencesKey("speaking_locale_tag")
    }
}
