package com.devansh.dhanwidget

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Matches the widget's fixed accent, used when the device can't supply a dynamic palette.
private val BrandBlue = Color(0xFF3D5AFE)

val GainGreen = Color(0xFF2E9E5B)
val LossRed = Color(0xFFE53E3E)

@Composable
fun DhanTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = BrandBlue)
        else -> lightColorScheme(primary = BrandBlue)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
