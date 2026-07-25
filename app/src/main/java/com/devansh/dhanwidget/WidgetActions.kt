package com.devansh.dhanwidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

val ViewModeKey = ActionParameters.Key<String>("view_mode")

class SetViewAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mode = parameters[ViewModeKey] ?: return
        updateAppWidgetState(context, glanceId) { prefs -> prefs[WidgetKeys.VIEW] = mode }
        HoldingsWidget().update(context, glanceId)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        markRefreshing(context, glanceId)
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }
}

private suspend fun markRefreshing(context: Context, glanceId: GlanceId) {
    updateAppWidgetState(context, glanceId) { prefs -> prefs[WidgetKeys.REFRESHING] = true }
    HoldingsWidget().update(context, glanceId)
}

/**
 * Same refresh the widget's own button performs, callable from the app: flips every placed widget
 * into its loading state before enqueuing the fetch, so both surfaces show the spinner.
 */
suspend fun requestWidgetRefresh(context: Context) {
    GlanceAppWidgetManager(context)
        .getGlanceIds(HoldingsWidget::class.java)
        .forEach { markRefreshing(context, it) }
    WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
}

class ToggleMaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetKeys.MASKED] = !(prefs[WidgetKeys.MASKED] ?: false)
        }
        HoldingsWidget().update(context, glanceId)
    }
}
