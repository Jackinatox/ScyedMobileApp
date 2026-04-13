/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.composestarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.android.wearable.composestarter.watchConfigDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigRepository(private val context: Context) {

    private val keyUrl = stringPreferencesKey("url")
    private val keyApiKey = stringPreferencesKey("api_key")

    val configFlow: Flow<WatchConfig?> =
        context.watchConfigDataStore.data.map { prefs ->
            val url = prefs[keyUrl]
            val apiKey = prefs[keyApiKey]
            if (url != null && apiKey != null) WatchConfig(url, apiKey) else null
        }

    suspend fun saveConfig(config: WatchConfig) {
        context.watchConfigDataStore.edit { prefs ->
            prefs[keyUrl] = config.url
            prefs[keyApiKey] = config.apiKey
        }
    }
}
