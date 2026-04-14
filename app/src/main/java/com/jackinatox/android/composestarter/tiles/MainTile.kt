package com.jackinatox.android.composestarter.tiles

import android.content.ComponentName
import androidx.wear.protolayout.ColorBuilders.ColorProp
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.Semantics
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.CardDefaults.filledTonalCardColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.titleCard
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.TypeBuilders.StringProp
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

class MainTile : Material3TileService(allowDynamicTheme = false) {
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val layout =
            when (val config = ConfigRepository(this@MainTile).configFlow.first()) {
                null ->
                    stateLayout(
                        title = "Pair phone",
                        message = "Missing URL/API key",
                    )

                else ->
                    DashboardRepository().fetchData(config).fold(
                        onSuccess = { dashboardLayout(it) },
                        onFailure = {
                            stateLayout(
                                title = "Fetch failed",
                                message = compactMessage(it.message ?: "Unknown error"),
                            )
                        },
                    )
            }

        return Tile.Builder()
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
            .setTileTimeline(Timeline.fromLayoutElement(layout))
            .build()
    }

    private fun MaterialScope.dashboardLayout(data: DashboardData): LayoutElement =
        primaryLayout(
            titleSlot = {
                text(
                    text =
                        "${apiSummary(data)} | ${dbSummary(data)} | ${formatTime(data.fetchedAt)}"
                            .layoutString,
                    color = colorScheme.onSurfaceVariant,
                    typography = Typography.LABEL_SMALL,
                )
            },
            mainSlot = { dashboardGrid(data) },
            onClick = openDashboardClickable("open-dashboard-root"),
        )

    private fun MaterialScope.dashboardGrid(data: DashboardData): LayoutElement =
        Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                metricRow(
                    left =
                        metricCard(
                            id = "metric-users",
                            value = shortNumber(data.usersTotal),
                            label = "USR",
                            valueColor = colorScheme.onSurface,
                            description = "Users ${data.usersTotal}",
                        ),
                    right =
                        metricCard(
                            id = "metric-servers",
                            value = "${data.gameServersActive}/${data.gameServersTotal}",
                            label = "SRV",
                            valueColor = serverColor(data),
                            description =
                                "Game servers ${data.gameServersActive} active of ${data.gameServersTotal}",
                        ),
                )
            )
            .addContent(verticalSpacer(8f))
            .addContent(
                metricRow(
                    left =
                        metricCard(
                            id = "metric-placeholder",
                            value = "--",
                            label = "NEXT",
                            valueColor = colorScheme.onSurfaceVariant,
                            description = "Placeholder for future metric",
                        ),
                    right =
                        metricCard(
                            id = "metric-tickets",
                            value = "${data.ticketsOpen}/${data.ticketsTotal}",
                            label = "TIX",
                            valueColor = ticketColor(data),
                            description =
                                "Support tickets ${data.ticketsOpen} open of ${data.ticketsTotal}",
                        ),
                )
            )
            .build()

    private fun metricRow(
        left: LayoutElement,
        right: LayoutElement,
    ): LayoutElement =
        Row.Builder()
            .setWidth(expand())
            .setHeight(weight(1f))
            .setVerticalAlignment(androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(left)
            .addContent(horizontalSpacer(8f))
            .addContent(right)
            .build()

    private fun MaterialScope.stateLayout(
        title: String,
        message: String,
    ): LayoutElement =
        primaryLayout(
            titleSlot = { text("Dashboard".layoutString, color = colorScheme.onSurfaceVariant) },
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

    private fun MaterialScope.metricCard(
        id: String,
        value: String,
        label: String,
        valueColor: LayoutColor,
        description: String,
    ): LayoutElement =
        Box.Builder()
            .setWidth(weight(1f))
            .setHeight(expand())
            .setHorizontalAlignment(androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(cardModifiers(id, description))
            .addContent(
                Column.Builder()
                    .setWidth(expand())
                    .setHorizontalAlignment(
                        androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
                    )
                    .addContent(
                        text(
                            text = value.layoutString,
                            color = valueColor,
                            typography = Typography.NUMERAL_SMALL,
                            maxLines = 1,
                        )
                    )
                    .addContent(verticalSpacer(2f))
                    .addContent(
                        text(
                            text = label.layoutString,
                            color = colorScheme.onSurfaceVariant,
                            typography = Typography.LABEL_SMALL,
                            maxLines = 1,
                        )
                    )
                    .build()
            )
            .build()

    private fun openDashboardClickable(id: String): Clickable =
        Clickable.Builder()
            .setId(id)
            .setOnClick(
                ActionBuilders.launchAction(ComponentName(this, MainActivity::class.java))
            )
            .build()

    private fun metricPadding(): Padding =
        Padding.Builder()
            .setStart(dp(8f))
            .setTop(dp(8f))
            .setEnd(dp(8f))
            .setBottom(dp(8f))
            .build()

    private fun MaterialScope.cardModifiers(
        id: String,
        description: String,
    ): Modifiers =
        Modifiers.Builder()
            .setClickable(openDashboardClickable(id))
            .setSemantics(
                Semantics.Builder()
                    .setContentDescription(StringProp.Builder(description).build())
                    .build()
            )
            .setPadding(metricPadding())
            .setBackground(
                Background.Builder()
                    .setColor(cardBackgroundColor())
                    .setCorner(Corner.Builder().setRadius(dp(18f)).build())
                    .build()
            )
            .build()

    private fun MaterialScope.cardBackgroundColor(): ColorProp = colorScheme.surfaceContainerHigh.prop

    private fun horizontalSpacer(widthDp: Float): LayoutElement =
        Spacer.Builder()
            .setWidth(dp(widthDp))
            .build()

    private fun verticalSpacer(heightDp: Float): LayoutElement =
        Spacer.Builder()
            .setHeight(dp(heightDp))
            .build()

    private fun apiSummary(data: DashboardData): String =
        if (data.status.equals("ok", ignoreCase = true)) "API OK" else "API ERR"

    private fun dbSummary(data: DashboardData): String =
        if (data.dbConnected) "DB ${compactDbLatency(data.dbLatency)}" else "DB DN"

    private fun MaterialScope.serverColor(data: DashboardData): LayoutColor = when {
        data.gameServersTotal == 0 -> colorScheme.onSurface
        data.gameServersExpired > 0 -> colorScheme.tertiary
        data.gameServersActive == data.gameServersTotal -> colorScheme.primary
        else -> colorScheme.onSurface
    }

    private fun MaterialScope.ticketColor(data: DashboardData): LayoutColor =
        if (data.ticketsOpen > 0) colorScheme.error else colorScheme.primary

    private fun compactDbLatency(value: String): String =
        value.replace(" ", "").replace("milliseconds", "ms", ignoreCase = true).take(4)

    private fun compactMessage(message: String): String =
        message.replace('\n', ' ').trim().take(48)

    private fun shortNumber(value: Int): String = when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000f)
        value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000f)
        else -> value.toString()
    }

    private fun formatTime(epochMs: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
}
