package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.labelRes
import com.nuvio.app.core.ui.isGradientTheme
import com.nuvio.app.isIos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.glass.isLiquidGlassAvailable
import nuvio.composeapp.generated.resources.settings_appearance_liquid_glass_effects
import nuvio.composeapp.generated.resources.settings_appearance_liquid_glass_effects_description
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.cd_selected
import nuvio.composeapp.generated.resources.settings_appearance_amoled_black
import nuvio.composeapp.generated.resources.settings_appearance_amoled_description
import nuvio.composeapp.generated.resources.settings_appearance_app_icon
import nuvio.composeapp.generated.resources.settings_appearance_app_language
import nuvio.composeapp.generated.resources.settings_appearance_app_language_restart_message
import nuvio.composeapp.generated.resources.settings_appearance_app_language_restart_title
import nuvio.composeapp.generated.resources.settings_appearance_app_language_sheet_title
import nuvio.composeapp.generated.resources.settings_appearance_section_display
import nuvio.composeapp.generated.resources.settings_appearance_section_theme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.appearanceSettingsContent(
    isTablet: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    appIconState: AppIconSettingsState,
    onAppIconSelected: (AppIconOption) -> Unit,
    onAppIconFailureDismissed: () -> Unit,
    selectedAppLanguage: AppLanguage,
    onAppLanguageSelected: (AppLanguage) -> Unit,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_theme),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                AppearanceThemeSelector(
                    isTablet = isTablet,
                    selectedTheme = selectedTheme,
                    onThemeSelected = onThemeSelected,
                )
                if (selectedTheme.isGradientTheme) {
                    SettingsGroupDivider(isTablet = isTablet)
                    ThemeGradientOpacityControl(isTablet = isTablet)
                }
            }
        }
    }
    appBackgroundSettingsItem(isTablet = isTablet)
    item {
        var showLanguageSheet by remember { mutableStateOf(false) }
        var showLanguageRestartDialog by remember { mutableStateOf(false) }
        val layoutDirection = LocalLayoutDirection.current
        var showAppIconPicker by remember { mutableStateOf(false) }
        val liquidGlassEffectsEnabled by ThemeSettingsRepository.liquidGlassEffectsEnabled.collectAsStateWithLifecycle()
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_display),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_appearance_amoled_black),
                    description = stringResource(Res.string.settings_appearance_amoled_description),
                    checked = amoledEnabled,
                    isTablet = isTablet,
                    onCheckedChange = onAmoledToggle,
                )
                if (isLiquidGlassAvailable()) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_appearance_liquid_glass_effects),
                        description = stringResource(Res.string.settings_appearance_liquid_glass_effects_description),
                        checked = liquidGlassEffectsEnabled,
                        isTablet = isTablet,
                        onCheckedChange = ThemeSettingsRepository::setLiquidGlassEffectsEnabled,
                    )
                }
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.settings_appearance_app_icon),
                    description = stringResource(appIconState.selected.labelResource),
                    enabled = appIconState.pending == null,
                    isTablet = isTablet,
                    trailingContent = {
                        AppIconThumbnail(
                            icon = appIconState.selected,
                            modifier = Modifier.size(if (isTablet) 44.dp else 40.dp),
                            cornerRadius = if (isTablet) 11.dp else 10.dp,
                        )
                    },
                    onClick = {
                        onAppIconFailureDismissed()
                        showAppIconPicker = true
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.settings_appearance_app_language),
                    description = stringResource(selectedAppLanguage.labelRes),
                    isTablet = isTablet,
                    onClick = { showLanguageSheet = true },
                )
            }
        }

        if (showLanguageSheet) {
            AppearanceLanguageBottomSheet(
                selectedLanguage = selectedAppLanguage,
                onLanguageSelected = {
                    onAppLanguageSelected(it)
                    showLanguageSheet = false
                    val newLayoutDirection = if (it.isRightToLeft()) LayoutDirection.Rtl else LayoutDirection.Ltr
                    if (isIos && newLayoutDirection != layoutDirection) showLanguageRestartDialog = true
                },
                onDismiss = { showLanguageSheet = false },
            )
        }

        NuvioStatusModal(
            title = stringResource(Res.string.settings_appearance_app_language_restart_title),
            message = stringResource(Res.string.settings_appearance_app_language_restart_message),
            isVisible = showLanguageRestartDialog,
            onConfirm = { showLanguageRestartDialog = false },
        )

        if (showAppIconPicker) {
            AppIconPicker(
                isTablet = isTablet,
                state = appIconState,
                onSelected = onAppIconSelected,
                onDismiss = {
                    onAppIconFailureDismissed()
                    showAppIconPicker = false
                },
            )
        }
    }
}

@Composable
private fun ThemeGradientOpacityControl(isTablet: Boolean) {
    val storedOpacity by ThemeSettingsRepository.themeGradientOpacity.collectAsStateWithLifecycle()
    var draftOpacity by remember { mutableFloatStateOf(storedOpacity) }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(storedOpacity) {
        if (!isDragging) draftOpacity = storedOpacity
    }
    val horizontalPadding = if (isTablet) 20.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = if (isTablet) 16.dp else 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Theme Gradient Opacity",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${(draftOpacity * 100f).roundToInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = draftOpacity,
            onValueChange = { value ->
                isDragging = true
                draftOpacity = value
                ThemeSettingsRepository.previewThemeGradientOpacity(value)
            },
            onValueChangeFinished = {
                isDragging = false
                ThemeSettingsRepository.saveThemeGradientOpacity(draftOpacity)
            },
            valueRange = 0.15f..1f,
            steps = 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(25, 50, 75, 100).forEach { percent ->
                TextButton(
                    onClick = {
                        val value = percent / 100f
                        isDragging = false
                        draftOpacity = value
                        ThemeSettingsRepository.saveThemeGradientOpacity(value)
                    },
                ) {
                    Text(text = "$percent%", color = Color.White)
                }
            }
        }
    }
}

private data class AppLanguageSheetOption(
    val language: AppLanguage,
    val labelRes: StringResource,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceLanguageBottomSheet(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val options = remember {
        AppLanguage.entries.map { language ->
            AppLanguageSheetOption(
                language = language,
                labelRes = language.labelRes,
            )
        }
    }

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.settings_appearance_app_language_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }

            items(options) { option ->
                NuvioBottomSheetActionRow(
                    title = stringResource(option.labelRes),
                    selected = option.language == selectedLanguage,
                    onClick = {
                        onLanguageSelected(option.language)
                        coroutineScope.launch {
                            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                    trailingContent = {
                        if (option.language == selectedLanguage) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(Res.string.cd_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}
