package com.devansh.dhanwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** How long to keep watching a running sync before giving up, so polling can never run unbounded. */
private const val SYNC_FOLLOW_TIMEOUT_MS = 30_000L

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DhanTheme {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var snapshot by remember { mutableStateOf(readWidgetSnapshotPlaceholder()) }
    var reloadTick by remember { mutableIntStateOf(0) }

    // Re-read whenever the screen comes to the foreground; no timer while it's backgrounded.
    LifecycleResumeEffect(Unit) {
        reloadTick++
        onPauseOrDispose { }
    }

    // One read per tick, then follow an in-flight sync to completion and stop. No steady-state polling.
    LaunchedEffect(reloadTick) {
        snapshot = readWidgetSnapshot(context)
        var waited = 0L
        while (snapshot.refreshing && waited < SYNC_FOLLOW_TIMEOUT_MS) {
            delay(1000)
            waited += 1000
            snapshot = readWidgetSnapshot(context)
        }
    }

    val refreshWidget: () -> Unit = {
        scope.launch {
            requestWidgetRefresh(context)
            reloadTick++
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Dhan Holdings") },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { Snackbar(snackbarData = it) }
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PortfolioHero(snapshot = snapshot, onRefresh = refreshWidget)
            ConnectionCard(
                tokenStore = tokenStore,
                onRefreshWidget = refreshWidget,
                onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            )
            AppearanceCard(tokenStore = tokenStore, onChanged = refreshWidget)
        }
    }
}

private fun readWidgetSnapshotPlaceholder() =
    WidgetSnapshot(summaries = null, refreshing = false, error = null, widgetPlaced = true)

@Composable
private fun PortfolioHero(snapshot: WidgetSnapshot, onRefresh: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Portfolio value",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (snapshot.summaries == null) "—" else formatInr(snapshot.combinedCurrent),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (snapshot.summaries != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChangeChip(label = "1D", pct = snapshot.dayChangePct)
                    Spacer(Modifier.width(8.dp))
                    ChangeChip(label = "Total", pct = snapshot.totalChangePct)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        !snapshot.widgetPlaced -> "Widget not added to home screen"
                        snapshot.error != null -> "Sync failed"
                        snapshot.summaries != null ->
                            "Synced ${timeOfDay(snapshot.summaries.updatedAtMillis)}"
                        else -> "Not synced yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (snapshot.error != null) LossRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRefresh, enabled = !snapshot.refreshing) {
                    Text(if (snapshot.refreshing) "Refreshing…" else "Refresh now")
                }
            }
            AnimatedVisibility(visible = snapshot.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ChangeChip(label: String, pct: Double) {
    val tint = if (pct >= 0) GainGreen else LossRed
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "%+.2f%%".format(pct),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

@Composable
private fun ConnectionCard(
    tokenStore: TokenStore,
    onRefreshWidget: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(tokenStore.accessToken.isNullOrBlank()) }
    var manualMode by remember { mutableStateOf(false) }

    var clientId by remember { mutableStateOf(tokenStore.clientId.orEmpty()) }
    var appId by remember { mutableStateOf(tokenStore.appId.orEmpty()) }
    var appSecret by remember { mutableStateOf(tokenStore.appSecret.orEmpty()) }
    var token by remember { mutableStateOf(tokenStore.accessToken.orEmpty()) }

    ExpandableCard(
        icon = painterResource(R.drawable.ic_trending),
        title = "Connection",
        subtitle = if (tokenStore.accessToken.isNullOrBlank()) "Not connected" else "Token saved",
        expanded = expanded,
        onToggle = { expanded = !expanded },
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !manualMode,
                onClick = { manualMode = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Dhan Login") },
            )
            SegmentedButton(
                selected = manualMode,
                onClick = { manualMode = true },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Manual token") },
            )
        }

        Spacer(Modifier.height(16.dp))

        if (manualMode) {
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://web.dhan.co/index/profile")),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Get token from Dhan") }
            Spacer(Modifier.height(12.dp))
            SecretField(label = "Access token", value = token, onValueChange = { token = it })
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    tokenStore.accessToken = token.trim()
                    onRefreshWidget()
                    onMessage("Token saved — refreshing widget")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = token.isNotBlank(),
            ) { Text("Save token") }
        } else {
            SecretField(label = "Client ID", value = clientId, onValueChange = { clientId = it })
            Spacer(Modifier.height(12.dp))
            SecretField(label = "App ID", value = appId, onValueChange = { appId = it })
            Spacer(Modifier.height(12.dp))
            SecretField(label = "App secret", value = appSecret, onValueChange = { appSecret = it })
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    tokenStore.clientId = clientId.trim()
                    tokenStore.appId = appId.trim()
                    tokenStore.appSecret = appSecret.trim()
                    context.startActivity(Intent(context, DhanLoginActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = clientId.isNotBlank() && appId.isNotBlank() && appSecret.isNotBlank(),
            ) { Text("Login with Dhan") }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Dhan tokens expire every 24 hours. Logging in again takes a few seconds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppearanceCard(tokenStore: TokenStore, onChanged: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var amoled by remember { mutableStateOf(tokenStore.amoledTheme) }

    ExpandableCard(
        icon = painterResource(R.drawable.ic_swap),
        title = "Appearance",
        subtitle = if (amoled) "AMOLED black" else "Follows system theme",
        expanded = expanded,
        onToggle = { expanded = !expanded },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AMOLED black theme", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Pure black widget background",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = amoled,
                onCheckedChange = {
                    amoled = it
                    tokenStore.amoledTheme = it
                    onChanged()
                },
            )
        }
    }
}

@Composable
private fun ExpandableCard(
    icon: Painter,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun SecretField(label: String, value: String, onValueChange: (String) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    painter = painterResource(
                        if (revealed) R.drawable.ic_eye_off else R.drawable.ic_eye,
                    ),
                    contentDescription = if (revealed) "Hide" else "Reveal",
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun formatInr(value: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return "₹${format.format(value)}"
}

private fun timeOfDay(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(millis)
