package com.devansh.dhanwidget

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "dhan_widget_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var amoledTheme: Boolean
        get() = prefs.getBoolean(KEY_AMOLED_THEME, false)
        set(value) = prefs.edit().putBoolean(KEY_AMOLED_THEME, value).apply()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_AMOLED_THEME = "amoled_theme"
    }
}
