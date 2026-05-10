package com.error404hsu.babymakisuk.featurehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.error404hsu.babymakisuk.coredata.repository.ChildRepository
import com.error404hsu.babymakisuk.coredata.repository.GrowthRepository
import com.error404hsu.babymakisuk.coremodel.ChildProfile
import com.error404hsu.babymakisuk.coremodel.Gender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val growthRepository: GrowthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun updateChild(child: ChildProfile) {
        viewModelScope.launch {
            childRepository.save(child)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            childRepository.observeAll().collect { children ->
                val boy = children.firstOrNull { it.gender == Gender.MALE }
                val girl = children.firstOrNull { it.gender == Gender.FEMALE }

                val boyRecords = boy?.let { growthRepository.observeByChild(it.id).first() } ?: emptyList()
                val girlRecords = girl?.let { growthRepository.observeByChild(it.id).first() } ?: emptyList()

                _uiState.update {
                    it.copy(
                        boy = boy,
                        girl = girl,
                        boyLatestGrowth = boyRecords.maxByOrNull { r -> r.date },
                        girlLatestGrowth = girlRecords.maxByOrNull { r -> r.date },
                        boyGrowthRecords = boyRecords.takeLast(6),
                        girlGrowthRecords = girlRecords.takeLast(6),
                        isLoading = false
                    )
                }
            }
        }
    }
}
