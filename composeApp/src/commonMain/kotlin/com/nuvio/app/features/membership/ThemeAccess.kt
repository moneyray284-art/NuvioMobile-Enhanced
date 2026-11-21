package com.nuvio.app.features.membership

import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.CustomThemeColors

private val supporterThemes = linkedMapOf(
    AppTheme.GOLD to CosmeticEntitlement.GOLD_THEME,
    AppTheme.JADE to CosmeticEntitlement.JADE_THEME,
    AppTheme.ROSE_GOLD to CosmeticEntitlement.ROSE_GOLD_THEME,
    AppTheme.ARCTIC_BLUE to CosmeticEntitlement.ARCTIC_BLUE_THEME,
    AppTheme.GRAPHITE to CosmeticEntitlement.GRAPHITE_THEME,
)


fun availableAppThemes(entitlements: CosmeticEntitlements): List<AppTheme> {
    return AppTheme.entries.filter { theme ->
        theme == AppTheme.CUSTOM || theme !in supporterThemes || entitlements.includes(supporterThemes.getValue(theme))
    }
}

fun resolveAppTheme(
    selectedTheme: AppTheme?,
    entitlements: CosmeticEntitlements,
): AppTheme {
    if (selectedTheme == null) {
        return supporterThemes
            .filterValues(entitlements::includes)
            .keys
            .firstOrNull()
            ?: AppTheme.WHITE
    }
    return selectedTheme.takeIf { it in availableAppThemes(entitlements) } ?: AppTheme.WHITE
}

fun resolveCustomThemeColors(colors: CustomThemeColors, memberTier: MemberTier?): CustomThemeColors =
    if (memberTier == null) CustomThemeColors.solid(colors.second) else colors
