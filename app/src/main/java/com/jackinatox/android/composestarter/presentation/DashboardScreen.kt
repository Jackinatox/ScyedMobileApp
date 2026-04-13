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
package com.jackinatox.android.composestarter.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun DashboardScreen(
    state: WatchUiState.Dashboard,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = onRefresh, buttonSize = EdgeButtonSize.ExtraSmall) {
                Text("Refresh")
            }
        },
        contentPadding =
            rememberResponsiveColumnPadding(
                first = ColumnItemType.ListHeader,
                last = ColumnItemType.Card,
            )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = modifier
        ) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth()) { Text("Dashboard") }
            }

            if (state.isRefreshing) {
                item { CircularProgressIndicator() }
            }

            if (state.error != null) {
                item {
                    TitleCard(
                        title = { Text("Error") },
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(state.error)
                    }
                }
            }

            state.data.forEach { (key, value) ->
                item(key = key) {
                    TitleCard(
                        title = { Text(key) },
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(value)
                    }
                }
            }
        }
    }
}
