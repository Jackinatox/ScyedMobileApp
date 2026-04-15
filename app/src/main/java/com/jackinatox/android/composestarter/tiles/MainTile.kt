package com.jackinatox.android.composestarter.tiles

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_LEFT
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_RIGHT
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.material3.CardDefaults.filledTonalCardColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.titleCard
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.jackinatox.android.composestarter.data.ConfigRepository
import com.jackinatox.android.composestarter.data.DashboardData
import com.jackinatox.android.composestarter.data.DashboardRepository
import com.jackinatox.android.composestarter.presentation.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

private const val FRESHNESS_INTERVAL_MS = 60_000L

// Each stat row is a single fixed-height line: no vertical stacking inside
// the row means zero clipping risk.
private const val ROW_HEIGHT_DP = 26f
private const val ROW_SPACER_DP = 3f
private const val ROW_PADDING_H_DP = 12f  // horizontal padding inside each card row

class MainTile : Material3TileService(allowDynamicTheme = false) {

    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val layout =
            when (val config = ConfigRepository(this@MainTile).configFlow.first()) {
                null ->
                    stateLayout(title = "Pair phone", message = "Missing URL/API key")
                else ->
                    DashboardRepository().fetchData(config).fold(
                        onSuccess = { dashboardLayout(it) },
                        onFailure = {
                            stateLayout(
                                title = "Fetch failed",
                                message = it.message?.replace('\n', ' ')?.trim()?.take(48)
                                    ?: "Unknown error",
                            )
                        },
                    )
            }

        return Tile.Builder()
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
            .setTileTimeline(Timeline.fromLayoutElement(layout))
            .build()
    }

    // ── Dashboard layout ───────────────────────────────────────────────────────

    private fun MaterialScope.dashboardLayout(data: DashboardData): LayoutElement =
        primaryLayout(
            titleSlot = {
                // Single-line status: time · API · DB
                text(
                    text = buildStatusLine(data).layoutString,
                    color = colorScheme.onSurfaceVariant,
                    typography = Typography.LABEL_SMALL,
                    maxLines = 1,
                )
            },
            mainSlot = { statList(data) },
            onClick = openDashboardClickable("open-dashboard"),
        )

    // ── Stat list: one row per metric, label left / value right ───────────────

    private fun MaterialScope.statList(data: DashboardData): LayoutElement =
        Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(
                statRow(
                    id = "row-players",
                    label = "players",
                    value = shortNumber(data.usersTotal),
                    valueColor = colorScheme.primary,
                    description = "Players ${data.usersTotal}",
                )
            )
            .addContent(rowSpacer())
            .addContent(
                statRow(
                    id = "row-servers",
                    label = "servers",
                    value = "${data.gameServersActive}/${data.gameServersTotal}",
                    valueColor = serverColor(data),
                    description =
                        "Servers ${data.gameServersActive} active of ${data.gameServersTotal}",
                )
            )
            .addContent(rowSpacer())
            .addContent(
                statRow(
                    id = "row-orders",
                    label = "orders",
                    value = "${data.ordersPending}/${data.ordersTotal}",
                    valueColor = ordersColor(data),
                    description =
                        "Orders ${data.ordersPending} pending of ${data.ordersTotal} total",
                )
            )
            .addContent(rowSpacer())
            .addContent(
                statRow(
                    id = "row-tickets",
                    label = "tickets",
                    value = "${data.ticketsOpen}/${data.ticketsTotal}",
                    valueColor = ticketColor(data),
                    description =
                        "Support tickets ${data.ticketsOpen} open of ${data.ticketsTotal}",
                )
            )
            .build()

    // ── Single stat row: one line, label left · value right ───────────────────
    //
    // Layout: Row(expand × ROW_HEIGHT_DP) with card background + H padding
    //   └─ Box(weight(1f), HORIZONTAL_ALIGN_LEFT)   ← label text
    //   └─ Box(weight(1f), HORIZONTAL_ALIGN_RIGHT)  ← value text
    //
    // Because each row is a single line of text at a fixed dp height, nothing
    // can overflow or clip — the text always fits within its allocated height.

    private fun MaterialScope.statRow(
        id: String,
        label: String,
        value: String,
        valueColor: LayoutColor,
        description: String,
    ): LayoutElement =
        Row.Builder()
            .setWidth(expand())
            .setHeight(dp(ROW_HEIGHT_DP))
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .setModifiers(rowModifiers(id, description))
            .addContent(labelBox(label))
            .addContent(valueBox(value, valueColor))
            .build()

    private fun MaterialScope.labelBox(label: String): LayoutElement =
        Box.Builder()
            .setWidth(weight(1f))
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_LEFT)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(
                text(
                    text = label.layoutString,
                    color = colorScheme.onSurfaceVariant,
                    typography = Typography.LABEL_MEDIUM,
                    maxLines = 1,
                )
            )
            .build()

    private fun MaterialScope.valueBox(value: String, color: LayoutColor): LayoutElement =
        Box.Builder()
            .setWidth(weight(1f))
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_RIGHT)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(
                text(
                    text = value.layoutString,
                    color = color,
                    typography = Typography.TITLE_SMALL,
                    maxLines = 1,
                )
            )
            .build()

    // ── State / error layout ───────────────────────────────────────────────────

    private fun MaterialScope.stateLayout(title: String, message: String): LayoutElement =
        primaryLayout(
            titleSlot = {
                text("Dashboard".layoutString, color = colorScheme.onSurfaceVariant)
            },
            mainSlot = {
                titleCard(
                    onClick = openDashboardClickable("open-dashboard-state"),
                    modifier = LayoutModifier.contentDescription("$title. $message"),
                    height = expand(),
                    colors = filledTonalCardColors(),
                    title = { text(title.layoutString) },
                    content = {
                        text(
                            text = message.layoutString,
                            typography = Typography.BODY_SMALL,
                            maxLines = 3,
                        )
                    },
                )
            },
            onClick = openDashboardClickable("open-dashboard-shell"),
        )

    // ── Modifiers ──────────────────────────────────────────────────────────────

    private fun openDashboardClickable(id: String): Clickable =
        Clickable.Builder()
            .setId(id)
            .setOnClick(ActionBuilders.launchAction(ComponentName(this, MainActivity::class.java)))
            .build()

    private fun MaterialScope.rowModifiers(id: String, description: String): Modifiers =
        Modifiers.Builder()
            .setClickable(openDashboardClickable(id))
            .setSemantics(
                Semantics.Builder()
                    .setContentDescription(StringProp.Builder(description).build())
                    .build()
            )
            .setPadding(
                Padding.Builder()
                    .setStart(dp(ROW_PADDING_H_DP))
                    .setEnd(dp(ROW_PADDING_H_DP))
                    .build()
            )
            .setBackground(
                Background.Builder()
                    .setColor(colorScheme.surfaceContainerHigh.prop)
                    .setCorner(Corner.Builder().setRadius(dp(12f)).build())
                    .build()
            )
            .build()

    // ── Colors ─────────────────────────────────────────────────────────────────

    private fun MaterialScope.serverColor(data: DashboardData): LayoutColor = when {
        data.gameServersTotal == 0 -> colorScheme.onSurface
        data.gameServersExpired > 0 -> colorScheme.tertiary
        data.gameServersActive == data.gameServersTotal -> colorScheme.primary
        else -> colorScheme.onSurface
    }

    private fun MaterialScope.ticketColor(data: DashboardData): LayoutColor =
        if (data.ticketsOpen > 0) colorScheme.error else colorScheme.primary

    private fun MaterialScope.ordersColor(data: DashboardData): LayoutColor =
        if (data.ordersPending > 0) colorScheme.tertiary else colorScheme.primary

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun rowSpacer(): LayoutElement =
        Spacer.Builder().setHeight(dp(ROW_SPACER_DP)).build()

    private fun shortNumber(value: Int): String = when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000f)
        value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000f)
        else -> value.toString()
    }

    private fun buildStatusLine(data: DashboardData): String {
        val api = if (data.status.equals("ok", ignoreCase = true)) "API OK" else "API ERR"
        val db = data.dbLatency
            .replace(" ", "")
            .replace("milliseconds", "ms", ignoreCase = true)
            .take(6)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(data.fetchedAt))
        return "$time · $api · DB $db"
    }
}
