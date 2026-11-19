package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_torbox_usage_active
import nuvio.composeapp.generated.resources.settings_torbox_usage_collapsed_days
import nuvio.composeapp.generated.resources.settings_torbox_usage_collapsed_free
import nuvio.composeapp.generated.resources.settings_torbox_usage_cooldown
import nuvio.composeapp.generated.resources.settings_torbox_usage_days_left
import nuvio.composeapp.generated.resources.settings_torbox_usage_monthly
import nuvio.composeapp.generated.resources.settings_torbox_usage_no_renewal
import nuvio.composeapp.generated.resources.settings_torbox_usage_plan_tier
import nuvio.composeapp.generated.resources.settings_torbox_usage_progress
import nuvio.composeapp.generated.resources.settings_torbox_usage_refresh
import nuvio.composeapp.generated.resources.settings_torbox_usage_renewal
import nuvio.composeapp.generated.resources.settings_torbox_usage_title
import nuvio.composeapp.generated.resources.settings_torbox_usage_today
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

    LaunchedEffect(apiKey) {
        TorboxUsageRepository.refresh(apiKey)
    }

    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val todayLabel = formatTorboxBytes(usageState.todayDownloadedBytes)
    val isFreeAccount = usageState.plan == null || usageState.plan == TorboxPlan.FREE
    val summary = if (isFreeAccount) {
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
            IconButton(
                onClick = {
                    scope.launch { TorboxUsageRepository.refresh(apiKey, force = true) }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.settings_torbox_usage_refresh),
                    tint = if (usageState.isLoading) tokens.colors.textDisabled else tokens.colors.textSecondary,
                )
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
                    TorboxPlanSummaryRow(state = usageState)
                    TorboxUsageStatsRow(state = usageState)
                    TorboxUsageProgress(state = usageState)
                    usageState.cooldownEpochMs?.let { cooldownEpochMs ->
                        TorboxCooldownBanner(cooldownEpochMs = cooldownEpochMs)
                    }
                }
            }
        }
    }
}

@Composable
private fun TorboxPlanSummaryRow(state: TorboxUsageUiState) {
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
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(tokens.colors.success, CircleShape),
                        )
                        Text(
                            text = stringResource(Res.string.settings_torbox_usage_active),
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.colors.success,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.settings_torbox_usage_renewal),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textMuted,
                )
                Text(
                    text = state.expiryEpochMs?.let(::formatTorboxDate)
                        ?: stringResource(Res.string.settings_torbox_usage_no_renewal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textSecondary,
                )
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
    val fraction = if (allowanceBytes <= 0L) {
        0f
    } else {
        (state.monthlyDownloadedBytes.toDouble() / allowanceBytes.toDouble())
            .toFloat()
            .coerceIn(0f, 1f)
    }
    val percent = if (allowanceBytes <= 0L) {
        0
    } else {
        ((state.monthlyDownloadedBytes.toDouble() / allowanceBytes.toDouble()) * 100.0)
            .roundToInt()
            .coerceAtLeast(0)
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
                color = tokens.colors.accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = tokens.colors.accent,
            trackColor = tokens.colors.borderSubtle,
        )
    }
}

@Composable
private fun TorboxCooldownBanner(cooldownEpochMs: Long) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(tokens.shapes.compactCard)
            .background(tokens.colors.warning.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                Res.string.settings_torbox_usage_cooldown,
                formatTorboxDateTime(cooldownEpochMs),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = tokens.colors.warning,
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
