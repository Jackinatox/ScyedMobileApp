package com.jackinatox.android.composestarter.tiles

import android.graphics.Color
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.jackinatox.android.composestarter.data.ConfigRepository
import com.jackinatox.android.composestarter.data.DashboardData
import com.jackinatox.android.composestarter.data.DashboardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val RESOURCES_VERSION = "1"
private const val FRESHNESS_INTERVAL_MS = 60_000L

private val GREEN = Color.parseColor("#6DD58C")
private val RED = Color.parseColor("#FF6B6B")
private val AMBER = Color.parseColor("#FFB74D")
private val CYAN = Color.parseColor("#7DD3FC")
private val WHITE = Color.WHITE
private val MUTED = Color.parseColor("#A7B0BE")
private val MUTED_DARK = Color.parseColor("#7D8794")
private val SURFACE = Color.parseColor("#101418")
private val BORDER = Color.parseColor("#27313A")
private val PANEL = Color.parseColor("#161D24")

class MainTile : TileService() {
    override fun onTileRequest(request: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        val root = renderTile(request.deviceConfiguration)
        val tile = Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(Layout.Builder().setRoot(root).build())
                            .build()
                    )
                    .build()
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(request: RequestBuilders.ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(
            Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )

    private fun renderTile(
        deviceParameters: DeviceParametersBuilders.DeviceParameters,
    ): LayoutElementBuilders.LayoutElement {
        val compact = deviceParameters.screenWidthDp <= 192
        val config = runBlocking { ConfigRepository(applicationContext).configFlow.first() }

        val body = when {
            config == null -> emptyState("Pair app", "Missing URL/API key", compact)
            else -> {
                val result = runBlocking { DashboardRepository().fetchData(config) }
                result.fold(
                    onSuccess = { dashboardLayout(it, compact) },
                    onFailure = { emptyState("Fetch failed", it.message ?: "Unknown error", compact) }
                )
            }
        }

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(SURFACE))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(16f)).build())
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(dp(if (compact) 20f else 16f))
                            .setTop(dp(if (compact) 18f else 14f))
                            .setEnd(dp(if (compact) 20f else 16f))
                            .setBottom(dp(if (compact) 18f else 14f))
                            .build()
                    )
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId("open-app")
                            .setOnClick(ActionBuilders.LaunchAction.Builder().build())
                            .build()
                    )
                    .build()
            )
            .addContent(body)
            .build()
    }

    private fun dashboardLayout(
        data: DashboardData,
        compact: Boolean,
    ): LayoutElementBuilders.LayoutElement {
        val statusColor = if (data.status.equals("ok", ignoreCase = true)) GREEN else RED
        val dbColor = if (data.dbConnected) GREEN else RED
        val ticketColor = if (data.ticketsOpen > 0) RED else GREEN
        val load = data.loadAverage.firstOrNull()?.let { "%.1f".format(Locale.US, it) } ?: "--"
        val syncedAt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(data.fetchedAt))
        val apiLabel = if (data.status.equals("ok", ignoreCase = true)) "API OK" else data.status.uppercase(Locale.getDefault())
        val dbLabel = if (data.dbConnected) data.dbLatency.ifBlank { "DB OK" } else "DB DOWN"

        return column(
            statusChip(apiLabel, dbLabel, syncedAt, statusColor, dbColor, compact),
            spacer(if (compact) 6f else 8f),
            metricBand(
                compactMetric("USR", shortNumber(data.usersTotal), WHITE, compact),
                compactMetric("SRV", "${data.gameServersActive}/${data.gameServersTotal}", GREEN, compact),
                compact
            ),
            spacer(4f),
            metricBand(
                compactMetric("ORD", "${data.ordersPending}/${data.ordersPaid}", AMBER, compact),
                compactMetric("TIX", "${data.ticketsOpen}/${data.ticketsTotal}", ticketColor, compact),
                compact
            ),
            spacer(4f),
            metricBand(
                compactMetric("LAT", "${data.latencyMs}ms", latencyColor(data.latencyMs), compact),
                compactMetric("RSS", trimValue(data.rss), WHITE, compact),
                compact
            ),
            spacer(4f),
            metricBand(
                compactMetric("GEO", shortNumber(data.locationsTotal), CYAN, compact),
                compactMetric("LD", load, loadColor(data.loadAverage.firstOrNull() ?: 0.0), compact),
                compact
            ),
            spacer(5f),
            centeredText(
                "${data.hostname.take(7)} ${trimValue(data.nodeVersion)} ${trimValue(data.processUptime)}",
                if (compact) 9f else 10f,
                MUTED,
                false
            )
        )
    }

    private fun emptyState(
        title: String,
        subtitle: String,
        compact: Boolean,
    ): LayoutElementBuilders.LayoutElement =
        column(
            topPill("DASHBOARD", compact),
            spacer(8f),
            centeredText(title, if (compact) 16f else 18f, WHITE, true),
            spacer(4f),
            centeredText(subtitle, if (compact) 11f else 13f, MUTED, false)
        )

    private fun compactMetric(
        label: String,
        value: String,
        color: Int,
        compact: Boolean,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Column.Builder()
            .setWidth(dp(if (compact) 42f else 48f))
            .addContent(text(label, 9f, MUTED_DARK, true))
            .addContent(text(value, if (compact) 15f else 16f, color, true))
            .build()

    private fun metricBand(
        left: LayoutElementBuilders.LayoutElement,
        right: LayoutElementBuilders.LayoutElement,
        compact: Boolean,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(PANEL))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(10f)).build())
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(dp(if (compact) 8f else 10f))
                            .setTop(dp(5f))
                            .setEnd(dp(if (compact) 8f else 10f))
                            .setBottom(dp(5f))
                            .build()
                    )
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Row.Builder()
                    .setWidth(expand())
                    .addContent(left)
                    .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(if (compact) 10f else 14f)).build())
                    .addContent(right)
                    .build()
            )
            .build()

    private fun statusChip(
        apiLabel: String,
        dbLabel: String,
        syncedAt: String,
        apiColor: Int,
        dbColor: Int,
        compact: Boolean,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(PANEL))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(14f)).build())
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(dp(8f))
                            .setTop(dp(5f))
                            .setEnd(dp(8f))
                            .setBottom(dp(5f))
                            .build()
                    )
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(expand())
                    .addContent(topPill("DASHBOARD", compact))
                    .addContent(spacer(3f))
                    .addContent(centeredText(apiLabel, if (compact) 12f else 13f, apiColor, true))
                    .addContent(spacer(1f))
                    .addContent(centeredText("$dbLabel  $syncedAt", if (compact) 9f else 10f, dbColor, false))
                    .build()
            )
            .build()

    private fun topPill(
        value: String,
        compact: Boolean,
    ): LayoutElementBuilders.LayoutElement =
        centeredText(value, if (compact) 8f else 9f, CYAN, true)

    private fun row(
        vararg children: LayoutElementBuilders.LayoutElement,
    ): LayoutElementBuilders.LayoutElement {
        val builder = LayoutElementBuilders.Row.Builder()
            .setWidth(expand())
        children.forEach(builder::addContent)
        return builder.build()
    }

    private fun column(
        vararg children: LayoutElementBuilders.LayoutElement,
    ): LayoutElementBuilders.LayoutElement {
        val builder = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
        children.forEach(builder::addContent)
        return builder.build()
    }

    private fun spacer(heightDp: Float): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Spacer.Builder()
            .setHeight(dp(heightDp))
            .build()

    private fun text(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setFontStyle(
                FontStyle.Builder()
                    .setColor(argb(color))
                    .setSize(sp(sizeSp))
                    .apply {
                        if (bold) {
                            setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        }
                    }
                    .build()
            )
            .setMaxLines(1)
            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
            .build()

    private fun centeredText(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .addContent(text(value, sizeSp, color, bold))
            .build()

    private fun shortNumber(value: Int): String = when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000f)
        value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000f)
        else -> value.toString()
    }

    private fun trimValue(value: String): String =
        value.replace("memory", "", ignoreCase = true)
            .replace("version", "v", ignoreCase = true)
            .trim()
            .take(9)

    private fun latencyColor(latencyMs: Long): Int = when {
        latencyMs < 150 -> GREEN
        latencyMs < 500 -> AMBER
        else -> RED
    }

    private fun loadColor(load: Double): Int = when {
        load < 0.7 -> GREEN
        load < 1.0 -> AMBER
        else -> RED
    }

    private fun dp(value: Float) = androidx.wear.protolayout.DimensionBuilders.dp(value)

    private fun sp(value: Float) = androidx.wear.protolayout.DimensionBuilders.sp(value)

    private fun expand() =
        androidx.wear.protolayout.DimensionBuilders.expand()

    private fun argb(value: Int) = ColorProp.Builder(value).build()
}
