package com.devansh.dhanwidget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetKeys {
    val SUMMARIES = stringPreferencesKey("summaries_json")
    val VIEW = stringPreferencesKey("view_mode")
    val ERROR = stringPreferencesKey("error")
    val MASKED = booleanPreferencesKey("masked")
    val TOP = booleanPreferencesKey("top_view")
    val AMOLED = booleanPreferencesKey("amoled")
    val REFRESHING = booleanPreferencesKey("refreshing")
}
