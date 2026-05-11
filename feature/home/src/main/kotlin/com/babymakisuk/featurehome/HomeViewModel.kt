package com.babymakisuk.featurehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.GrowthRepository
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val growthRepository: GrowthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            seedDefaultChildrenIfEmpty()
            loadData()
        }
    }

    // ─── Seed 預設資料（僅當資料庫完全空白時執行一次）─────────────────
    private suspend fun seedDefaultChildrenIfEmpty() {
        val existing = childRepository.observeAll().first()
        if (existing.isNotEmpty()) return  // 已有資料，跳過

        val defaultBoy = ChildProfile(
            id        = "default_boy",                        // 固定 ID，確保不重複新增
            name      = "小明",
            gender    = Gender.MALE,
            birthDate = LocalDate.now().minusMonths(6),      // 預設 6 個月大
            photoUrl  = null
        )
        val defaultGirl = ChildProfile(
            id        = "default_girl",
            name      = "小美",
            gender    = Gender.FEMALE,
            birthDate = LocalDate.now().minusMonths(8),      // 預設 8 個月大
            photoUrl  = null
        )

        childRepository.save(defaultBoy)
        childRepository.save(defaultGirl)
    }

    // ─── 更新單一孩子資料（由 UI 呼叫）──────────────────────────────────
    fun updateChild(child: ChildProfile) {
        viewModelScope.launch {
            childRepository.save(child)
        }
    }

    // ─── 載入資料並更新 UI State ─────────────────────────────────────────
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            childRepository.observeAll().collect { children ->
                val boy  = children.firstOrNull { it.gender == Gender.MALE }
                val girl = children.firstOrNull { it.gender == Gender.FEMALE }

                val boyRecords  = boy?.let  { growthRepository.observeByChild(it.id).first() } ?: emptyList()
                val girlRecords = girl?.let { growthRepository.observeByChild(it.id).first() } ?: emptyList()

                _uiState.update {
                    it.copy(
                        boy               = boy,
                        girl              = girl,
                        boyLatestGrowth   = boyRecords.maxByOrNull  { r -> r.date },
                        girlLatestGrowth  = girlRecords.maxByOrNull { r -> r.date },
                        boyGrowthRecords  = boyRecords.takeLast(6),
                        girlGrowthRecords = girlRecords.takeLast(6),
                        isLoading         = false
                    )
                }
            }
        }
    }
}
