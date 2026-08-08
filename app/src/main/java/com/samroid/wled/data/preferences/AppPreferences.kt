package com.samroid.wled.data.preferences

import androidx.datastore.preferences.core.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject



class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {


    companion object {

        val THEME =
            stringPreferencesKey("theme")


        val LANGUAGE =
            stringPreferencesKey("language")

    }



    val theme: Flow<String> =
        dataStore.data.map {

                preferences ->

            preferences[THEME] ?: "DARK"

        }



    val language: Flow<String> =
        dataStore.data.map {

                preferences ->

            preferences[LANGUAGE] ?: "fa"

        }



    suspend fun saveTheme(
        value:String
    ){

        dataStore.edit {

            it[THEME] = value

        }

    }



    suspend fun saveLanguage(
        value:String
    ){

        dataStore.edit {

            it[LANGUAGE] = value

        }

    }

}