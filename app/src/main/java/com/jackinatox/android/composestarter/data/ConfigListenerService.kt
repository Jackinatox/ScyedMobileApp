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

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


private const val TAG = "WearConfigListner"

class ConfigListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val configRepository by lazy { ConfigRepository(applicationContext) }

    override fun onMessageReceived(message: MessageEvent) {
        Log.d(TAG, "── Message received: path=${message.path}  size=${message.data.size}B")
        if (message.path == "/config/initialSetup") {
            try {
                val raw = String(message.data, Charsets.UTF_8)
                Log.d(TAG, "│ raw payload: $raw")
                val json = JSONObject(raw)
                val url = json.getString("url")
                val apiKey = json.getString("api_key")
                Log.i(TAG, "✓ Config parsed  url=$url  key=${apiKey.take(4)}****")
                scope.launch { configRepository.saveConfig(WatchConfig(url, apiKey)) }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to parse config message", e)
            }
        } else {
            Log.d(TAG, "Ignoring unhandled path: ${message.path}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
