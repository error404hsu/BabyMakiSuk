package com.babymakisuk.featurehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.GrowthRepository
import com.babymakisuk.coredata.repository.ToiletRepository
import com.babymakisuk.coredata.repository.VaccineReminderRepository
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.ToiletRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val growthRepository: GrowthRepository,
    private val toiletRepository: ToiletRepository,
    private val vaccineReminderRepository: VaccineReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            seedDefaultChildrenIfEmpty()
            loadData()
        }
    }

    private suspend fun seedDefaultChildrenIfEmpty() {
        val existing = childRepository.observeAll().first()
        if (existing.isNotEmpty()) return

        val defaultBoy = ChildProfile(
            id        = 1L,
            name      = "小明",
            gender    = Gender.MALE,
            birthday = LocalDate.now().minusMonths(6),
        )
        val defaultGirl = ChildProfile(
            id        = 2L,
            name      = "小美",
            gender    = Gender.FEMALE,
            birthday = LocalDate.now().minusMonths(8),
        )

        childRepository.save(defaultBoy)
        childRepository.save(defaultGirl)
    }

    fun updateChild(child: ChildProfile) {
        viewModelScope.launch {
            childRepository.save(child)
        }
    }

    fun logToilet(childId: Long) {
        viewModelScope.launch {
            toiletRepository.insertToilet(ToiletRecord(childId = childId))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            childRepository.observeAll()
                .flatMapLatest { children ->
                    val boy = children.firstOrNull { it.gender == Gender.MALE }
                    val girl = children.firstOrNull { it.gender == Gender.FEMALE }

                    val boyToiletFlow = boy?.let { toiletRepository.getToiletRecords(it.id) }
                        ?: flowOf(emptyList())
                    val girlToiletFlow = girl?.let { toiletRepository.getToiletRecords(it.id) }
                        ?: flowOf(emptyList())

                    combine(boyToiletFlow, girlToiletFlow) { boyToilet, girlToilet ->
                        Triple(children, boyToilet, girlToilet)
                    }
                }
                .collect { (children, boyToilet, girlToilet) ->
                    val boy = children.firstOrNull { it.gender == Gender.MALE }
                    val girl = children.firstOrNull { it.gender == Gender.FEMALE }

                    val boyRecords = boy?.let { growthRepository.observeByChild(it.id).first() } ?: emptyList()
                    val girlRecords = girl?.let { growthRepository.observeByChild(it.id).first() } ?: emptyList()

                    val boyNextVaccine = boy?.let { vaccineReminderRepository.getNextDue(it.id) }
                    val girlNextVaccine = girl?.let { vaccineReminderRepository.getNextDue(it.id) }

                    _uiState.update {
                        it.copy(
                            boy = boy,
                            girl = girl,
                            boyLatestGrowth = boyRecords.maxByOrNull { r -> r.date },
                            girlLatestGrowth = girlRecords.maxByOrNull { r -> r.date },
                            boyGrowthRecords = boyRecords.takeLast(6),
                            girlGrowthRecords = girlRecords.takeLast(6),
                            boyToiletRecords = boyToilet,
                            girlToiletRecords = girlToilet,
                            boyNextVaccine = boyNextVaccine,
                            girlNextVaccine = girlNextVaccine,
                            isLoading = false
                        )
                    }
                }
        }
    }
}
