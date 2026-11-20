package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.i18n.localizedByteUnit
import com.nuvio.app.core.i18n.localizedShortMonthName
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.debrid.TorboxPlan
import com.nuvio.app.features.debrid.TorboxUsageRepository
import com.nuvio.app.features.debrid.TorboxUsageUiState
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_torbox_usage_active
import nuvio.composeapp.generated.resources.settings_torbox_usage_auto_renew
import nuvio.composeapp.generated.resources.settings_torbox_usage_collapsed_days
import nuvio.composeapp.generated.resources.settings_torbox_usage_collapsed_free
import nuvio.composeapp.generated.resources.settings_torbox_usage_cooldown
import nuvio.composeapp.generated.resources.settings_torbox_usage_days_left
import nuvio.composeapp.generated.resources.settings_torbox_usage_expired
import nuvio.composeapp.generated.resources.settings_torbox_usage_free
import nuvio.composeapp.generated.resources.settings_torbox_usage_linked_account
import nuvio.composeapp.generated.resources.settings_torbox_usage_load_failed
import nuvio.composeapp.generated.resources.settings_torbox_usage_monthly
import nuvio.composeapp.generated.resources.settings_torbox_usage_no_renewal
import nuvio.composeapp.generated.resources.settings_torbox_usage_plan_tier
import nuvio.composeapp.generated.resources.settings_torbox_usage_progress
import nuvio.composeapp.generated.resources.settings_torbox_usage_refresh
import nuvio.composeapp.generated.resources.settings_torbox_usage_renewal
import nuvio.composeapp.generated.resources.settings_torbox_usage_retry
import nuvio.composeapp.generated.resources.settings_torbox_usage_title
import nuvio.composeapp.generated.resources.settings_torbox_usage_today
import nuvio.composeapp.generated.resources.settings_torbox_usage_upgrade
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TorboxUsageSettingsCard(
    isTablet: Boolean,
    apiKey: String,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val usageState by remember {
        TorboxUsageRepository.ensureLoaded()
        TorboxUsageRepository.uiState
    }.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val tokens = MaterialTheme.nuvio
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(apiKey) {
        TorboxUsageRepository.refresh(apiKey)
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            TorboxUsageRepository.refreshUsageWindow()
        }
    }

    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val todayLabel = formatTorboxBytes(usageState.todayDownloadedBytes)
    val isActive = usageState.isActivePlan()
    val isFreeAccount = !isActive && usageState.plan == TorboxPlan.FREE
    val summary = if (isFreeAccount || usageState.plan == null) {
        stringResource(Res.string.settings_torbox_usage_collapsed_free, todayLabel)
    } else {
        stringResource(
            Res.string.settings_torbox_usage_collapsed_days,
            (usageState.daysLeft ?: 0L).toString(),
            todayLabel,
        )
    }

    SettingsGroup(isTablet = isTablet) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_torbox_usage_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (expanded) {
                IconButton(
                    onClick = {
                        scope.launch { TorboxUsageRepository.refresh(apiKey, force = true) }
                    },
                    enabled = !usageState.isLoading,
                ) {
                    if (usageState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = tokens.colors.accent,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.settings_torbox_usage_refresh),
                            tint = tokens.colors.textSecondary,
                        )
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = tokens.colors.textMuted,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    thickness = tokens.borders.hairline,
                    color = tokens.colors.borderSubtle,
                )
                Column(
                    modifier = Modifier.padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = verticalPadding,
                        bottom = verticalPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    usageState.email?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } ?: Text(
                        text = stringResource(Res.string.settings_torbox_usage_linked_account),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                    )
                    if (usageState.loadFailed) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = usageState.errorMessage
                                    ?: stringResource(Res.string.settings_torbox_usage_load_failed),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.colors.danger,
                            )
                            TextButton(
                                onClick = { scope.launch { TorboxUsageRepository.refresh(apiKey, force = true) } },
                            ) {
                                Text(stringResource(Res.string.settings_torbox_usage_retry))
                            }
                        }
                    }
                    TorboxPlanSummaryRow(
                        state = usageState,
                        onUpgradeClick = { runCatching { uriHandler.openUri("https://torbox.app/") } },
                    )
                    TorboxUsageStatsRow(state = usageState)
                    TorboxUsageProgress(state = usageState)
                    usageState.cooldownEpochMs
                        ?.takeIf { !usageState.isActivePlan() && it > Clock.System.now().toEpochMilliseconds() }
                        ?.let { cooldownEpochMs ->
                            Text(
                                text = stringResource(
                                    Res.string.settings_torbox_usage_cooldown,
                                    formatTorboxDateTime(cooldownEpochMs),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.colors.textMuted,
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun TorboxPlanSummaryRow(
    state: TorboxUsageUiState,
    onUpgradeClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val plan = state.plan
    val daysLeft = state.daysLeft
    val isActive = state.isSubscribed ||
        (plan != null && plan != TorboxPlan.FREE && (daysLeft ?: 0L) > 0L)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.settings_torbox_usage_plan_tier),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textMuted,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = plan?.displayName ?: "—",
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val statusString = when {
                        state.isSubscribed -> Res.string.settings_torbox_usage_auto_renew
                        isActive -> Res.string.settings_torbox_usage_active
                        plan == TorboxPlan.FREE -> Res.string.settings_torbox_usage_free
                        else -> Res.string.settings_torbox_usage_expired
                    }
                    Text(
                        text = stringResource(statusString),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) tokens.colors.success else tokens.colors.textMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.settings_torbox_usage_renewal),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textMuted,
                )
                val showNoRenewal = !isActive && !state.isSubscribed && plan == TorboxPlan.FREE &&
                    (state.expiryEpochMs == null || state.expiryEpochMs <= Clock.System.now().toEpochMilliseconds())
                if (showNoRenewal) {
                    Text(
                        text = stringResource(Res.string.settings_torbox_usage_no_renewal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                    )
                    TextButton(onClick = onUpgradeClick) {
                        Text(stringResource(Res.string.settings_torbox_usage_upgrade))
                    }
                } else {
                    Text(
                        text = state.expiryEpochMs?.let(::formatTorboxDate)
                            ?: stringResource(Res.string.settings_torbox_usage_no_renewal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                    )
                }
            }
        }
        RoundRenewalTimer(daysLeft = daysLeft)
    }
}

@Composable
private fun RoundRenewalTimer(daysLeft: Long?) {
    val tokens = MaterialTheme.nuvio
    val timerColor = when {
        daysLeft == null -> tokens.colors.textMuted
        daysLeft <= 7L -> tokens.colors.danger
        daysLeft <= 30L -> tokens.colors.warning
        else -> tokens.colors.success
    }
    val progress = when {
        daysLeft == null || daysLeft <= 0L -> 0f
        daysLeft >= 30L -> 1f
        else -> daysLeft.toFloat() / 30f
    }
    val trackColor = tokens.colors.borderSubtle

    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.minDimension - strokeWidth, size.minDimension - strokeWidth)
            drawCircle(
                color = trackColor,
                radius = size.minDimension / 2f - inset,
                style = Stroke(width = strokeWidth),
            )
            if (progress > 0f) {
                drawArc(
                    color = timerColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = daysLeft?.toString() ?: "—",
                fontSize = 15.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.colors.textPrimary,
            )
            Text(
                text = stringResource(Res.string.settings_torbox_usage_days_left),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = tokens.colors.textMuted,
            )
        }
    }
}

@Composable
private fun TorboxUsageStatsRow(state: TorboxUsageUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TorboxUsageStat(
            label = stringResource(Res.string.settings_torbox_usage_today),
            bytes = state.todayDownloadedBytes,
            modifier = Modifier.weight(1f),
        )
        TorboxUsageStat(
            label = stringResource(Res.string.settings_torbox_usage_monthly),
            bytes = state.monthlyDownloadedBytes,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TorboxUsageStat(
    label: String,
    bytes: Long,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textMuted,
        )
        Text(
            text = formatTorboxBytes(bytes),
            style = MaterialTheme.typography.titleMedium,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TorboxUsageProgress(state: TorboxUsageUiState) {
    val tokens = MaterialTheme.nuvio
    val allowanceBytes = (state.plan ?: TorboxPlan.FREE).monthlyAllowanceBytes
    val usageRatio = if (allowanceBytes <= 0L) {
        0f
    } else {
        (state.monthlyDownloadedBytes.toDouble() / allowanceBytes.toDouble())
            .toFloat()
    }
    val fraction = usageRatio.coerceIn(0f, 1f)
    val percent = if (allowanceBytes <= 0L) {
        0
    } else {
        ((state.monthlyDownloadedBytes.toDouble() / allowanceBytes.toDouble()) * 100.0)
            .roundToInt()
            .coerceAtLeast(0)
    }
    val progressColor = when {
        usageRatio >= 0.9f -> tokens.colors.danger
        usageRatio >= 0.75f -> tokens.colors.warning
        else -> tokens.colors.success
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    Res.string.settings_torbox_usage_progress,
                    formatTorboxBytes(state.monthlyDownloadedBytes),
                    formatTorboxBytes(allowanceBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall,
                color = progressColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = progressColor,
            trackColor = tokens.colors.borderSubtle,
        )
    }
}

private val TorboxByteUnits = listOf(
    "PB" to 1_000_000_000_000_000.0,
    "TB" to 1_000_000_000_000.0,
    "GB" to 1_000_000_000.0,
    "MB" to 1_000_000.0,
    "KB" to 1_000.0,
)

private fun formatTorboxBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 ${localizedByteUnit("B")}"
    val value = bytes.toDouble()
    val unit = TorboxByteUnits.firstOrNull { value >= it.second }
        ?: return "$bytes ${localizedByteUnit("B")}"
    val scaled = floor((value / unit.second) * 10.0) / 10.0
    val number = if (scaled >= 100.0 || scaled % 1.0 == 0.0) {
        scaled.toLong().toString()
    } else {
        scaled.toString()
    }
    val label = when (unit.first) {
        "KB", "MB", "GB" -> localizedByteUnit(unit.first)
        else -> unit.first
    }
    return "$number $label"
}

private fun formatTorboxDate(epochMs: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return "${date.dayOfMonth} ${localizedShortMonthName(date.monthNumber)} ${date.year}"
}

private fun formatTorboxDateTime(epochMs: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    val date = dateTime.date
    return "${date.dayOfMonth} ${localizedShortMonthName(date.monthNumber)} ${date.year}, $hour:$minute"
}

private fun TorboxUsageUiState.isActivePlan(): Boolean = isSubscribed ||
    (plan != null && plan != TorboxPlan.FREE && (daysLeft ?: 0L) > 0L)
