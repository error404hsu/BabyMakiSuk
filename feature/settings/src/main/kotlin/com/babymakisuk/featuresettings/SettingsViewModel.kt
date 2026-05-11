package com.babymakisuk.featuresettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coredata.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val darkMode = repository.darkModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DarkModeOption.SYSTEM
    )

    fun setDarkMode(option: DarkModeOption) {
        viewModelScope.launch { repository.setDarkMode(option) }
    }

    fun exportData(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportAllDataAsJson()
            onComplete(json)
        }
    }

    fun importData(jsonString: String) {
        viewModelScope.launch {
            repository.importDataFromJson(jsonString)
        }
    }
}
