package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.debrid.TorboxAccountUsage
import com.nuvio.app.features.debrid.TorboxUsageRepository
import com.nuvio.app.features.debrid.formatTorboxBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_debrid_torbox_active
import nuvio.composeapp.generated.resources.settings_debrid_torbox_auto_renew
import nuvio.composeapp.generated.resources.settings_debrid_torbox_cooldown
import nuvio.composeapp.generated.resources.settings_debrid_torbox_days_left
import nuvio.composeapp.generated.resources.settings_debrid_torbox_download_count
import nuvio.composeapp.generated.resources.settings_debrid_torbox_download_count_plural
import nuvio.composeapp.generated.resources.settings_debrid_torbox_expiry_unavailable
import nuvio.composeapp.generated.resources.settings_debrid_torbox_free_status
import nuvio.composeapp.generated.resources.settings_debrid_torbox_linked_account
import nuvio.composeapp.generated.resources.settings_debrid_torbox_load_failed
import nuvio.composeapp.generated.resources.settings_debrid_torbox_monthly
import nuvio.composeapp.generated.resources.settings_debrid_torbox_no_active_renewal
import nuvio.composeapp.generated.resources.settings_debrid_torbox_plan_tier
import nuvio.composeapp.generated.resources.settings_debrid_torbox_refresh
import nuvio.composeapp.generated.resources.settings_debrid_torbox_retry
import nuvio.composeapp.generated.resources.settings_debrid_torbox_renewal
import nuvio.composeapp.generated.resources.settings_debrid_torbox_today
import nuvio.composeapp.generated.resources.settings_debrid_torbox_total
import nuvio.composeapp.generated.resources.settings_debrid_torbox_upgrade
import nuvio.composeapp.generated.resources.settings_debrid_torbox_usage
import nuvio.composeapp.generated.resources.settings_debrid_torbox_usage_problem
import nuvio.composeapp.generated.resources.settings_debrid_torbox_usage_unavailable
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val TORBOX_ACCOUNT_URL = "https://torbox.app/"

@Composable
internal fun TorboxUsageSettingsCard(
    apiKey: String,
    isTablet: Boolean,
) {
    var uiState by remember(apiKey) { mutableStateOf(TorboxUsageCardState()) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val loadFailedMessage = stringResource(Res.string.settings_debrid_torbox_load_failed)
    val tokens = MaterialTheme.nuvio

    suspend fun refreshUsage() {
        if (uiState.isLoading) return
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        try {
            val usage = TorboxUsageRepository.fetchAccountUsage(apiKey)
            uiState = TorboxUsageCardState(usage = usage)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = error.message?.takeIf(String::isNotBlank) ?: loadFailedMessage,
            )
        }
    }

    LaunchedEffect(apiKey) {
        refreshUsage()
    }

    SettingsGroup(isTablet = isTablet) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TorBox",
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = uiState.usage?.email?.takeIf(String::isNotBlank)
                            ?: stringResource(Res.string.settings_debrid_torbox_linked_account),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { scope.launch { refreshUsage() } },
                    enabled = !uiState.isLoading,
                ) {
                    if (uiState.isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = tokens.colors.accent,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.settings_debrid_torbox_refresh),
                            tint = tokens.colors.textMuted,
                        )
                    }
                }
            }

            uiState.errorMessage?.let { errorMessage ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.danger,
                    )
                    TextButton(
                        onClick = { scope.launch { refreshUsage() } },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(stringResource(Res.string.settings_debrid_torbox_retry))
                    }
                }
            }

            uiState.usage?.let { usage ->
                HorizontalDivider(color = tokens.colors.borderSubtle)
                PlanAndRenewalRow(
                    usage = usage,
                    onUpgradeClick = { runCatching { uriHandler.openUri(TORBOX_ACCOUNT_URL) } },
                )
                HorizontalDivider(color = tokens.colors.borderSubtle)
                UsageSummary(usage = usage)
                usage.usageErrorMessage?.let { errorMessage ->
                    Text(
                        text = stringResource(
                            Res.string.settings_debrid_torbox_usage_problem,
                            errorMessage,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.warning,
                    )
                }
                if (usage.cooldownUntil != null) {
                    Text(
                        text = stringResource(
                            Res.string.settings_debrid_torbox_cooldown,
                            formatTorboxDate(usage.cooldownUntil),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanAndRenewalRow(
    usage: TorboxAccountUsage,
    onUpgradeClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_debrid_torbox_plan_tier),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.textMuted,
            )
            Text(
                text = usage.planDisplayName,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    when {
                        usage.isSubscribed -> Res.string.settings_debrid_torbox_auto_renew
                        usage.isActivePlan -> Res.string.settings_debrid_torbox_active
                        else -> Res.string.settings_debrid_torbox_free_status
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = if (usage.isSubscribed || usage.isActivePlan) {
                    tokens.colors.success
                } else {
                    tokens.colors.textMuted
                },
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_debrid_torbox_renewal),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.textMuted,
            )
            if (!usage.isSubscribed && !usage.isActivePlan) {
                Text(
                    text = stringResource(Res.string.settings_debrid_torbox_no_active_renewal),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textPrimary,
                )
                TextButton(
                    onClick = onUpgradeClick,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_debrid_torbox_upgrade),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = usage.premiumExpiresAt?.let(::formatTorboxDate)
                            ?: stringResource(Res.string.settings_debrid_torbox_expiry_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    usage.daysUntilRenewal?.let { daysLeft ->
                        Text(
                            text = stringResource(Res.string.settings_debrid_torbox_days_left, daysLeft),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                daysLeft <= 7L -> tokens.colors.danger
                                daysLeft <= 30L -> tokens.colors.warning
                                else -> tokens.colors.success
                            },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageSummary(usage: TorboxAccountUsage) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UsageStat(
            label = stringResource(Res.string.settings_debrid_torbox_today),
            value = usage.todayDownloadedBytes?.let(::formatTorboxBytes) ?: "—",
            modifier = Modifier.weight(1f),
        )
        UsageStat(
            label = stringResource(Res.string.settings_debrid_torbox_monthly),
            value = usage.monthlyDownloadedBytes?.let(::formatTorboxBytes) ?: "—",
            modifier = Modifier.weight(1f),
        )
        UsageStat(
            label = stringResource(Res.string.settings_debrid_torbox_total),
            value = usage.totalDownloadedBytes?.let(::formatTorboxBytes)
                ?: stringResource(
                    if (usage.totalDownloads == 1L) {
                        Res.string.settings_debrid_torbox_download_count
                    } else {
                        Res.string.settings_debrid_torbox_download_count_plural
                    },
                    usage.totalDownloads,
                ),
            valueColor = tokens.colors.accent,
            modifier = Modifier.weight(1f),
        )
    }

    val ratio = usage.monthlyUsageRatio
    val monthlyBytes = usage.monthlyDownloadedBytes
    if (ratio == null || monthlyBytes == null) {
        Text(
            text = stringResource(Res.string.settings_debrid_torbox_usage_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = tokens.colors.textMuted,
        )
        return
    }

    val percentage = (ratio * 100f).roundToInt().coerceAtLeast(0)
    val barColor = when {
        ratio >= 0.9f -> tokens.colors.danger
        ratio >= 0.75f -> tokens.colors.warning
        else -> tokens.colors.success
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    Res.string.settings_debrid_torbox_usage,
                    formatTorboxBytes(monthlyBytes),
                    formatTorboxBytes(usage.plan.allowanceBytes),
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.textPrimary,
                fontSize = 11.sp,
            )
        }
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = barColor,
            trackColor = tokens.colors.surfaceElevated,
        )
    }
}

@Composable
private fun UsageStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.nuvio.colors.textPrimary,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.nuvio.colors.textMuted,
            maxLines = 2,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class TorboxUsageCardState(
    val usage: TorboxAccountUsage? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

private fun formatTorboxDate(value: String): String {
    val instant = runCatching { Instant.parse(value) }.getOrNull()
        ?: return value.take(10).ifBlank { value }
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = TORBOX_MONTHS.getOrNull(date.monthNumber - 1) ?: return date.toString()
    return "${date.day.toString().padStart(2, '0')} $month ${date.year}"
}

private val TORBOX_MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
