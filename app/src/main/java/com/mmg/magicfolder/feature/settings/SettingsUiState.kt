package com.mmg.magicfolder.feature.settings

import com.mmg.magicfolder.core.domain.model.AppLanguage
import com.mmg.magicfolder.core.domain.model.CardLanguage
import com.mmg.magicfolder.core.domain.model.NewsLanguage
import com.mmg.magicfolder.core.domain.model.PreferredCurrency
import com.mmg.magicfolder.core.domain.model.UserPreferences
import com.mmg.magicfolder.core.ui.theme.AppTheme

data class SettingsUiState(
    val autoRefreshPrices: Boolean = false,
    val currentTheme: AppTheme = AppTheme.NeonVoid,
)

data class PreferencesState(
    val userPreferences: UserPreferences = UserPreferences(
        appLanguage = AppLanguage.ENGLISH,
        cardLanguage = CardLanguage.ENGLISH,
        newsLanguages = setOf(NewsLanguage.ENGLISH),
        preferredCurrency = PreferredCurrency.USD,
    )
)
