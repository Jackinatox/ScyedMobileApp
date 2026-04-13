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
package com.example.android.wearable.composestarter.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.wearable.composestarter.data.ConfigRepository
import com.example.android.wearable.composestarter.data.DashboardRepository
import com.example.android.wearable.composestarter.data.WatchConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WatchUiState {
    object Loading : WatchUiState

    object NoPairing : WatchUiState

    data class Dashboard(
        val data: Map<String, String>,
        val isRefreshing: Boolean,
        val error: String?,
    ) : WatchUiState
}

class MainViewModel(
    private val configRepository: ConfigRepository,
    private val dashboardRepository: DashboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchUiState>(WatchUiState.Loading)
    val uiState: StateFlow<WatchUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            configRepository.configFlow.collect { config ->
                refreshJob?.cancel()
                if (config == null) {
                    _uiState.value = WatchUiState.NoPairing
                } else {
                    startRefreshLoop(config)
                }
            }
        }
    }

    fun refreshNow() {
        val current = _uiState.value
        if (current is WatchUiState.Dashboard) {
            refreshJob?.cancel()
            // Re-read config from the flow's last emission via a one-shot launch
            viewModelScope.launch {
                configRepository.configFlow.collect { config ->
                    if (config != null) {
                        startRefreshLoop(config)
                    }
                    return@collect
                }
            }
        }
    }

    private fun startRefreshLoop(config: WatchConfig) {
        refreshJob =
            viewModelScope.launch {
                while (true) {
                    val current = _uiState.value
                    val currentData =
                        if (current is WatchUiState.Dashboard) current.data else emptyMap()
                    _uiState.value =
                        WatchUiState.Dashboard(
                            data = currentData,
                            isRefreshing = true,
                            error = null
                        )

                    val result = dashboardRepository.fetchData(config)
                    _uiState.value =
                        if (result.isSuccess) {
                            WatchUiState.Dashboard(
                                data = result.getOrThrow(),
                                isRefreshing = false,
                                error = null
                            )
                        } else {
                            WatchUiState.Dashboard(
                                data = currentData,
                                isRefreshing = false,
                                error = result.exceptionOrNull()?.message ?: "Unknown error"
                            )
                        }

                    delay(5 * 60_000L)
                }
            }
    }
}
