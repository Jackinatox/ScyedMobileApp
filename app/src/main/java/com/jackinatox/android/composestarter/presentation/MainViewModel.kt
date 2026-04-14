package com.jackinatox.android.composestarter.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jackinatox.android.composestarter.data.ConfigRepository
import com.jackinatox.android.composestarter.data.DashboardData
import com.jackinatox.android.composestarter.data.DashboardRepository
import com.jackinatox.android.composestarter.data.WatchConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

sealed interface WatchUiState {
    object Loading : WatchUiState

    object NoPairing : WatchUiState

    data class Dashboard(
        val dashboardData: DashboardData?,   // null = no data received yet
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
        Log.d(TAG, "ViewModel initialized")
        viewModelScope.launch {
            configRepository.configFlow.collect { config ->
                refreshJob?.cancel()
                if (config == null) {
                    Log.i(TAG, "Config cleared → NoPairing")
                    _uiState.value = WatchUiState.NoPairing
                } else {
                    Log.i(TAG, "Config received → starting refresh loop for ${config.url}")
                    startRefreshLoop(config)
                }
            }
        }
    }

    fun refreshNow() {
        if (_uiState.value is WatchUiState.Dashboard) {
            Log.d(TAG, "Manual refresh triggered")
            refreshJob?.cancel()
            viewModelScope.launch {
                configRepository.configFlow.collect { config ->
                    if (config != null) startRefreshLoop(config)
                    return@collect
                }
            }
        }
    }

    private fun startRefreshLoop(config: WatchConfig) {
        refreshJob = viewModelScope.launch {
            while (true) {
                val prevData = (_uiState.value as? WatchUiState.Dashboard)?.dashboardData
                _uiState.value = WatchUiState.Dashboard(
                    dashboardData = prevData,
                    isRefreshing = true,
                    error = null,
                )

                val result = dashboardRepository.fetchData(config)

                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    Log.i(TAG, "Fetch SUCCESS — ${data.latencyMs}ms")
                    _uiState.value = WatchUiState.Dashboard(
                        dashboardData = data,
                        isRefreshing = false,
                        error = null,
                    )
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.w(TAG, "Fetch FAILED — $msg")
                    _uiState.value = WatchUiState.Dashboard(
                        dashboardData = prevData,
                        isRefreshing = false,
                        error = msg,
                    )
                }

                Log.d(TAG, "Next refresh in 5 minutes")
                delay(5 * 60_000L)
            }
        }
    }
}
