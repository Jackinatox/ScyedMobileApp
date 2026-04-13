# Phone Companion App — Implementation Guide

The watch app is ready. This document describes exactly what the Android phone companion app must do to send configuration (URL + API key) to the watch.

---

## Overview

The phone app sends a URL and API key to the watch using the **Wearable Data Layer API** (Google Play Services). Once received, the watch persists the config and immediately begins fetching data from that URL.

---

## Project Setup

### 1. Create a new Android module (or project)

If adding as a module inside this project:

```groovy
// settings.gradle — already has :app (watch), add:
include ":phone"
```

The phone app's `build.gradle` must set:

```kotlin
android {
    defaultConfig {
        // CRITICAL: must exactly match the watch app's applicationId
        applicationId = "com.example.android.wearable.composestarter"
    }
}
```

> The Wearable Data Layer routes messages by `applicationId`. If the IDs differ,
> the watch will never receive the data.

### 2. Dependencies

```kotlin
dependencies {
    // Compose (standard Material 3, NOT Wear)
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

    // Wearable Data Layer
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
}
```

### 3. AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Tells the system this app may communicate with a Wear OS app -->
    <queries>
        <package android:name="com.google.android.wearable.app" />
    </queries>

    <application
        android:label="Watch Companion"
        android:theme="@style/Theme.Material3.DayNight">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

No `INTERNET` permission needed — the phone only sends data via DataClient (no direct networking).

---

## Data Layer Protocol

| Property      | Value                    |
|---------------|--------------------------|
| DataItem path | `/watch/config`          |
| Key: url      | `"url"` (String)         |
| Key: api_key  | `"api_key"` (String)     |
| Transport     | `PutDataMapRequest` with `.setUrgent()` |

`.setUrgent()` bypasses the 30-second delivery batching — the watch gets the config within a few seconds of the user tapping "Send".

---

## Implementation

### `data/ConfigSender.kt`

```kotlin
import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class ConfigSender(context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    suspend fun sendConfig(url: String, apiKey: String): Result<Unit> {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return Result.failure(Exception("No paired watch found. Make sure your watch is connected."))
            }

            val request = PutDataMapRequest.create("/watch/config").apply {
                dataMap.putString("url", url)
                dataMap.putString("api_key", apiKey)
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### `presentation/SetupViewModel.kt`

```kotlin
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SetupUiState {
    object Idle : SetupUiState
    object Sending : SetupUiState
    object Success : SetupUiState
    data class Error(val message: String) : SetupUiState
}

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val sender = ConfigSender(application)

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Idle)
    val uiState: StateFlow<SetupUiState> = _uiState

    var url by mutableStateOf("")
    var apiKey by mutableStateOf("")

    fun sendConfig() {
        viewModelScope.launch {
            _uiState.value = SetupUiState.Sending
            val result = sender.sendConfig(url.trim(), apiKey.trim())
            _uiState.value =
                if (result.isSuccess) SetupUiState.Success
                else SetupUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    fun resetState() {
        _uiState.value = SetupUiState.Idle
    }
}
```

### `presentation/SetupScreen.kt`

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SetupScreen(viewModel: SetupViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configure Watch",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Enter the data source URL and API key. These will be sent to your paired watch.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = viewModel.url,
            onValueChange = { viewModel.url = it },
            label = { Text("Data URL") },
            placeholder = { Text("https://api.example.com/data") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = viewModel.apiKey,
            onValueChange = { viewModel.apiKey = it },
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.sendConfig() },
            enabled = uiState !is SetupUiState.Sending && viewModel.url.isNotBlank() && viewModel.apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is SetupUiState.Sending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Send to Watch")
            }
        }

        when (val state = uiState) {
            is SetupUiState.Success ->
                Text(
                    text = "Sent! Your watch will update shortly.",
                    color = MaterialTheme.colorScheme.primary
                )
            is SetupUiState.Error ->
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            else -> {}
        }
    }
}
```

### `MainActivity.kt`

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    private val viewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    SetupScreen(viewModel)
                }
            }
        }
    }
}
```

---

## How it works end-to-end

1. User opens phone app, enters URL + API key, taps **Send to Watch**.
2. `ConfigSender` checks for connected watch nodes. If none → shows error.
3. `PutDataMapRequest` writes `url` and `api_key` to path `/watch/config` and marks it urgent.
4. The watch's `ConfigListenerService` receives the `DATA_CHANGED` event.
5. It saves the config to DataStore (persisted across reboots).
6. `ConfigRepository.configFlow` emits the new `WatchConfig`.
7. `MainViewModel` cancels the old refresh loop, starts a new one with the new config.
8. The watch transitions from `PairingScreen` → `DashboardScreen` and immediately fetches data.

---

## Testing tips

- **No watch connected**: The button shows "No paired watch found." — make sure Bluetooth is on and the watch is paired via the Wear OS app.
- **Wrong applicationId**: If you see no error but the watch never updates, double-check that both apps share the same `applicationId` exactly.
- **Watch receives old config**: `PutDataItem` is idempotent — if you send the same URL + API key twice, the watch only triggers `onDataChanged` once. Change at least one character to force a new event.
- **Watch not running**: That's fine — the Data Layer caches the DataItem and delivers it when the watch app next runs.
