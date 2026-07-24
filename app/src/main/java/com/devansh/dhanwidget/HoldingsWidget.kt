package com.devansh.dhanwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale

private val PositiveColor = ColorProvider(Color(0xFF2E9E5B))
private val NegativeColor = ColorProvider(Color(0xFFE53E3E))
private val AccentColor = ColorProvider(Color(0xFF3D5AFE))
private val NumberFont = FontFamily("sans-serif-medium")

private data class Palette(
    val background: ColorProvider,
    val onBackground: ColorProvider,
    val muted: ColorProvider,
)

private val AmoledPalette = Palette(
    background = ColorProvider(Color.Black),
    onBackground = ColorProvider(Color.White),
    muted = ColorProvider(Color(0xFF9A9A9A)),
)

class HoldingsWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val error = prefs[WidgetKeys.ERROR]
        val masked = prefs[WidgetKeys.MASKED] ?: false
        val amoled = prefs[WidgetKeys.AMOLED] ?: false
        val refreshing = prefs[WidgetKeys.REFRESHING] ?: false
        val viewMode = prefs[WidgetKeys.VIEW] ?: "STOCKS"
        val summaries = prefs[WidgetKeys.SUMMARIES]?.let { Json.decodeFromString<StoredSummaries>(it) }
        val summary = summaries?.let { if (viewMode == "STOCKS") it.stocks else it.etfs }

        val palette = if (amoled) {
            AmoledPalette
        } else {
            Palette(
                background = GlanceTheme.colors.widgetBackground,
                onBackground = GlanceTheme.colors.onSurface,
                muted = GlanceTheme.colors.outline,
            )
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(palette.background)
                .cornerRadius(20.dp)
                .padding(16.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (viewMode == "STOCKS") "Stocks" else "ETFs",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontFamily = NumberFont,
                        fontSize = 14.sp,
                        color = palette.onBackground,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = summaries?.let { "· ${timeLabel(it.updatedAtMillis)}" } ?: "",
                    style = TextStyle(color = palette.muted, fontSize = 12.sp),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Image(
                    provider = ImageProvider(R.drawable.ic_swap),
                    contentDescription = "Switch Stocks/ETFs",
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(
                            actionRunCallback<SetViewAction>(
                                actionParametersOf(ViewModeKey to if (viewMode == "STOCKS") "ETF" else "STOCKS"),
                            ),
                        ),
                    colorFilter = ColorFilter.tint(AccentColor),
                )
                Spacer(modifier = GlanceModifier.width(14.dp))
                Image(
                    provider = ImageProvider(if (masked) R.drawable.ic_eye_off else R.drawable.ic_eye),
                    contentDescription = "Show or hide values",
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<ToggleMaskAction>()),
                    colorFilter = ColorFilter.tint(palette.onBackground),
                )
                Spacer(modifier = GlanceModifier.width(14.dp))
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = GlanceModifier.size(20.dp),
                        color = AccentColor,
                    )
                } else {
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = "Refresh",
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(actionRunCallback<RefreshAction>()),
                        colorFilter = ColorFilter.tint(palette.onBackground),
                    )
                }
            }

            when {
                error != null -> Text(
                    text = "Error: $error — tap to open settings",
                    modifier = GlanceModifier
                        .padding(top = 12.dp)
                        .clickable(actionStartActivity<SettingsActivity>()),
                    style = TextStyle(color = NegativeColor),
                )
                summary == null -> Text(
                    text = "No data yet — tap refresh",
                    modifier = GlanceModifier.padding(top = 12.dp),
                    style = TextStyle(color = palette.onBackground),
                )
                summary.holdingsCount == 0 -> Text(
                    text = "No ${if (viewMode == "STOCKS") "stocks" else "ETFs"} in this portfolio",
                    modifier = GlanceModifier.padding(top = 12.dp),
                    style = TextStyle(color = palette.muted),
                )
                else -> Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 18.dp)) {
                    Text(
                        text = if (masked) "•  •  •  •  •  •  •" else "₹%,.0f".format(summary.currentValue),
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontFamily = NumberFont,
                            fontSize = 32.sp,
                            color = palette.onBackground,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(20.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        StatBlock(
                            label = "1D returns",
                            value = summary.currentValue - summary.prevCloseValue,
                            pct = summary.dayChangePct,
                            masked = masked,
                            palette = palette,
                            alignEnd = false,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        StatBlock(
                            label = "Total returns",
                            value = summary.currentValue - summary.investedValue,
                            pct = summary.overallChangePct,
                            masked = masked,
                            palette = palette,
                            alignEnd = true,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatBlock(
        label: String,
        value: Double,
        pct: Double,
        masked: Boolean,
        palette: Palette,
        alignEnd: Boolean,
        modifier: GlanceModifier,
    ) {
        val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
        Column(
            modifier = modifier,
            horizontalAlignment = if (alignEnd) Alignment.Horizontal.End else Alignment.Horizontal.Start,
        ) {
            Text(
                text = label,
                style = TextStyle(color = palette.muted, fontSize = 13.sp, textAlign = textAlign),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = if (masked) {
                    "•  •  •  • (%+.2f%%)".format(pct)
                } else {
                    "₹%+,.0f (%+.2f%%)".format(value, pct)
                },
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontFamily = NumberFont,
                    fontSize = 16.sp,
                    textAlign = textAlign,
                    color = if (pct >= 0) PositiveColor else NegativeColor,
                ),
            )
        }
    }
}

private fun timeLabel(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(millis)
