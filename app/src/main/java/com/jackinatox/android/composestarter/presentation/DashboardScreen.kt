package com.jackinatox.android.composestarter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import com.jackinatox.android.composestarter.data.DashboardData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Colors ────────────────────────────────────────────────────────────────────

private val Green  = Color(0xFF4CAF50)
private val Red    = Color(0xFFF44336)
private val Blue   = Color(0xFF64B5F6)
private val Orange = Color(0xFFFFB74D)
private val Teal   = Color(0xFF4DB6AC)

private fun latencyColor(ms: Long) = when {
    ms < 150 -> Green
    ms < 500 -> Orange
    else     -> Red
}

private fun formatTime(epochMs: Long): String =
    if (epochMs == 0L) "--:--"
    else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochMs))

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(
    state: WatchUiState.Dashboard,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val data = state.dashboardData

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(onClick = onRefresh, buttonSize = EdgeButtonSize.ExtraSmall) {
                if (state.isRefreshing) CircularProgressIndicator(modifier = Modifier.size(14.dp))
                else Text("Sync")
            }
        },
        contentPadding = rememberResponsiveColumnPadding(
            first = ColumnItemType.ListHeader,
            last = ColumnItemType.Card,
        )
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = modifier,
        ) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth()) {
                    Text("Dashboard", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            if (state.error != null) {
                item { ErrorCard(state.error) }
            }

            if (state.isRefreshing && data == null) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            }

            if (data != null) {
                item { StatusBar(data, state.isRefreshing) }
                item { PlayersServersCard(data, Modifier.fillMaxWidth()) }
                item { OrdersCard(data, Modifier.fillMaxWidth()) }
                item { SupportCard(data, Modifier.fillMaxWidth()) }
                item { SystemCard(data, Modifier.fillMaxWidth()) }
                item { MemoryCard(data, Modifier.fillMaxWidth()) }
                item { CpuCard(data, Modifier.fillMaxWidth()) }
            }
        }
    }
}

// ── Status bar: status dot · sync time · DB latency ──────────────────────────

@Composable
private fun StatusBar(data: DashboardData, isRefreshing: Boolean) {
    val statusOk  = data.status.lowercase() == "ok"
    val statusColor = if (statusOk) Green else Red
    val dbColor     = if (data.dbConnected) Green else Red

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
            Text(data.status, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
        }

        Dot()

        // Sync time
        StatCell(
            value = if (isRefreshing) "···" else formatTime(data.fetchedAt),
            label = "synced",
            color = MaterialTheme.colorScheme.primary,
        )

        Dot()

        // DB latency
        StatCell(value = data.dbLatency, label = "DB", color = dbColor)
    }
}

// ── Players & Servers ─────────────────────────────────────────────────────────

@Composable
private fun PlayersServersCard(data: DashboardData, modifier: Modifier = Modifier) {
    TitleCard(
        title = {
            Text("Players & Servers", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onClick = {},
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${data.usersTotal}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blue,
                )
                Text("players", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(Modifier.width(1.dp).height(44.dp).background(MaterialTheme.colorScheme.outline))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${data.gameServersActive}/${data.gameServersTotal}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green,
                )
                Text("active servers", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (data.gameServersExpired > 0) {
                    Text(
                        "${data.gameServersExpired} expired",
                        fontSize = 9.sp,
                        color = Orange,
                    )
                }
            }
        }
    }
}

// ── Orders ────────────────────────────────────────────────────────────────────

@Composable
private fun OrdersCard(data: DashboardData, modifier: Modifier = Modifier) {
    TitleCard(
        title = {
            Text("Orders", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onClick = {},
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MiniStat("${data.ordersTotal}", "total", MaterialTheme.colorScheme.primary)
            MiniStat("${data.ordersPending}", "pending", Orange)
            MiniStat("${data.ordersPaid}", "paid", Green)
        }
    }
}

// ── System ────────────────────────────────────────────────────────────────────

@Composable
private fun SystemCard(data: DashboardData, modifier: Modifier = Modifier) {
    TitleCard(
        title = {
            Text(
                "System · ${data.hostname}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick = {},
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            KeyValue("server up", data.systemUptime)
            KeyValue("process", data.processUptime)
            KeyValue("node", data.nodeVersion)
            KeyValue("env", data.nodeEnv)
        }
    }
}

// ── Memory ────────────────────────────────────────────────────────────────────

@Composable
private fun MemoryCard(data: DashboardData, modifier: Modifier = Modifier) {
    TitleCard(
        title = {
            Text("Memory", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onClick = {},
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            KeyValue("RSS", data.rss, color = Teal)
            KeyValue("heap used", data.heapUsed)
            KeyValue("system free", data.freeMemory)
            KeyValue("system total", data.totalMemory)
        }
    }
}

// ── CPU Load ──────────────────────────────────────────────────────────────────

@Composable
private fun CpuCard(data: DashboardData, modifier: Modifier = Modifier) {
    TitleCard(
        title = {
            Text(
                "CPU Load · ${data.cpuCount} cores",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = {},
        modifier = modifier,
    ) {
        if (data.loadAverage.size >= 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LoadCell(data.loadAverage[0], "1m")
                LoadCell(data.loadAverage[1], "5m")
                LoadCell(data.loadAverage[2], "15m")
            }
        }
    }
}

@Composable
private fun LoadCell(load: Double, label: String) {
    val color = when {
        load < 0.5 -> Green
        load < 1.0 -> Orange
        else       -> Red
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("%.2f".format(load), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Support Tickets ───────────────────────────────────────────────────────────

@Composable
private fun SupportCard(data: DashboardData, modifier: Modifier = Modifier) {
    val hasIssues = data.ticketsOpen > 0
    val dotColor  = if (hasIssues) Red else Green

    TitleCard(
        title = {
            Text("Support", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onClick = {},
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Text(
                if (hasIssues) "${data.ticketsOpen} open" else "No open tickets",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = dotColor,
            )
        }
    }
}

// ── Error card ────────────────────────────────────────────────────────────────

@Composable
private fun ErrorCard(error: String) {
    TitleCard(
        title = { Text("Error", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) },
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(error, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun Dot() {
    Box(Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
}

@Composable
private fun StatCell(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KeyValue(
    key: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(key, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color)
    }
}
