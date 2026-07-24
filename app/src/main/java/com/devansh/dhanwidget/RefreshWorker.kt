package com.devansh.dhanwidget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tokenStore = TokenStore(applicationContext)
        val repository = HoldingsRepository(DhanApiFactory.create(), YahooFinanceApiFactory.create(), tokenStore)
        val glanceIds = GlanceAppWidgetManager(applicationContext).getGlanceIds(HoldingsWidget::class.java)
        if (glanceIds.isEmpty()) return Result.success()

        val result = runCatching { repository.fetch() }

        glanceIds.forEach { id ->
            updateAppWidgetState(applicationContext, id) { prefs ->
                prefs[WidgetKeys.AMOLED] = tokenStore.amoledTheme
                prefs[WidgetKeys.REFRESHING] = false
                result.onSuccess { summaries ->
                    prefs[WidgetKeys.SUMMARIES] = Json.encodeToString(summaries)
                    prefs.remove(WidgetKeys.ERROR)
                }.onFailure { e ->
                    val body = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()
                    Log.e("RefreshWorker", "fetch failed, body=$body", e)
                    prefs[WidgetKeys.ERROR] = e.message ?: e.toString()
                }
            }
            HoldingsWidget().update(applicationContext, id)
        }
        return Result.success()
    }
}

fun schedulePeriodicRefresh(context: Context) {
    val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "holdings_refresh",
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}
