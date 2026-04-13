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
package com.jackinatox.android.composestarter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

class DashboardRepository {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchData(config: WatchConfig): Result<Map<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val request =
                    Request.Builder()
                        .url(config.url)
                        .addHeader("X-API-Key", config.apiKey)
                        .build()

                val body =
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(
                                Exception("HTTP ${response.code}: ${response.message}")
                            )
                        }
                        response.body?.string()
                            ?: return@withContext Result.failure(Exception("Empty response body"))
                    }

                val parsed =
                    json
                        .parseToJsonElement(body)
                        .jsonObject
                        .mapValues { it.value.toString().trim('"') }

                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
