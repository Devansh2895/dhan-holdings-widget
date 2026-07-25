package com.devansh.dhanwidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.serialization.json.Json

data class WidgetSnapshot(
    val summaries: StoredSummaries?,
    val refreshing: Boolean,
    val error: String?,
    val widgetPlaced: Boolean,
) {
    val combinedCurrent: Double
        get() = (summaries?.stocks?.currentValue ?: 0.0) + (summaries?.etfs?.currentValue ?: 0.0)

    val combinedInvested: Double
        get() = (summaries?.stocks?.investedValue ?: 0.0) + (summaries?.etfs?.investedValue ?: 0.0)

    val combinedPrevClose: Double
        get() = (summaries?.stocks?.prevCloseValue ?: 0.0) + (summaries?.etfs?.prevCloseValue ?: 0.0)

    val dayChangePct: Double
        get() = if (combinedPrevClose == 0.0) 0.0 else (combinedCurrent - combinedPrevClose) / combinedPrevClose * 100

    val totalChangePct: Double
        get() = if (combinedInvested == 0.0) 0.0 else (combinedCurrent - combinedInvested) / combinedInvested * 100
}

/** Reads the widget's own Glance state so the app can mirror what the widget is showing. */
suspend fun readWidgetSnapshot(context: Context): WidgetSnapshot {
    val glanceId = runCatching {
        GlanceAppWidgetManager(context).getGlanceIds(HoldingsWidget::class.java).firstOrNull()
    }.getOrNull() ?: return WidgetSnapshot(null, refreshing = false, error = null, widgetPlaced = false)

    val prefs = runCatching {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
    }.getOrNull() ?: return WidgetSnapshot(null, refreshing = false, error = null, widgetPlaced = true)

    val summaries = prefs[WidgetKeys.SUMMARIES]
        ?.let { runCatching { Json.decodeFromString<StoredSummaries>(it) }.getOrNull() }

    return WidgetSnapshot(
        summaries = summaries,
        refreshing = prefs[WidgetKeys.REFRESHING] ?: false,
        error = prefs[WidgetKeys.ERROR],
        widgetPlaced = true,
    )
}
