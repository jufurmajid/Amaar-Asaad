package com.amaarasaad.store.data.repository

import com.amaarasaad.store.data.model.StoreSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface StoreSettingsRepository {
    val settings: Flow<StoreSettings>
    suspend fun getSettings(): StoreSettings
    suspend fun updateSettings(newSettings: StoreSettings)
}

class StoreSettingsRepositoryImpl(
    initialSettings: StoreSettings = StoreSettings()
) : StoreSettingsRepository {

    private val _settings = MutableStateFlow(initialSettings)
    override val settings: Flow<StoreSettings> = _settings.asStateFlow()

    override suspend fun getSettings(): StoreSettings = _settings.value

    override suspend fun updateSettings(newSettings: StoreSettings) {
        _settings.update { newSettings }
    }
}
