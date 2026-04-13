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

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConfigListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val configRepository by lazy { ConfigRepository(applicationContext) }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (
                event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == "/watch/config"
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val url = dataMap.getString("url") ?: return@forEach
                val apiKey = dataMap.getString("api_key") ?: return@forEach
                scope.launch { configRepository.saveConfig(WatchConfig(url, apiKey)) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
