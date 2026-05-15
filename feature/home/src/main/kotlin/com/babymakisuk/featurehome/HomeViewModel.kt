package com.babymakisuk.featurehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.GrowthRepository
import com.babymakisuk.coredata.repository.MemoRepository
import com.babymakisuk.coredata.repository.SystemReminderRepository
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
    private val vaccineReminderRepository: VaccineReminderRepository,
    private val memoRepository: MemoRepository,
    private val medicalDao: MedicalDao,
    private val systemReminderRepository: SystemReminderRepository
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
            checkLongNoBm(childId)
        }
    }

    private suspend fun checkLongNoBm(childId: Long) {
        val latestTime = toiletRepository.getLatestToiletTime(childId) ?: return
        val hoursSince = ((System.currentTimeMillis() - latestTime) / 3600000).toInt()
        if (hoursSince >= 48) {
            val existing = systemReminderRepository.getUnresolvedByType(childId, com.babymakisuk.coremodel.SystemReminderType.LONG_NO_BM).first()
            if (existing.isEmpty()) {
                systemReminderRepository.createLongNoBmReminder(childId, hoursSince)
            }
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

                    val boyMedicalFlow = boy?.let { medicalDao.observeByChild(it.id) }
                        ?: flowOf(emptyList())
                    val girlMedicalFlow = girl?.let { medicalDao.observeByChild(it.id) }
                        ?: flowOf(emptyList())

                    val todayDate = LocalDate.now().toEpochDay()
                    val todayMemosFlow = memoRepository.getByDate(todayDate)
                        .let { flowOf(it) }

                    combine(
                        boyToiletFlow, 
                        girlToiletFlow, 
                        boyMedicalFlow, 
                        girlMedicalFlow, 
                        todayMemosFlow
                    ) { boyToilet, girlToilet, boyMedical, girlMedical, todayMemos ->
                        DataTuple(children, boyToilet, girlToilet, boyMedical, girlMedical, todayMemos)
                    }
                }
                .collect { tuple ->
                    val children = tuple.children
                    val boyToilet = tuple.boyToilet
                    val girlToilet = tuple.girlToilet
                    val boyMedical = tuple.boyMedical
                    val girlMedical = tuple.girlMedical
                    val todayMemos = tuple.todayMemos
                    
                    val boy = children.firstOrNull { it.gender == Gender.MALE }
                    val girl = children.firstOrNull { it.gender == Gender.FEMALE }

                    val boyRecords = boy?.let { growthRepository.observeByChild(it.id).first() } ?: emptyList()
                    val girlRecords = girl?.let { growthRepository.observeByChild(it.id).first() } ?: emptyList()

                    val boyNextVaccine = boy?.let { vaccineReminderRepository.getNextDue(it.id) }
                    val girlNextVaccine = girl?.let { vaccineReminderRepository.getNextDue(it.id) }

                    val todayMemosByChild = todayMemos.groupBy { m -> m.childId }

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
                            boyLatestMedical = boyMedical.firstOrNull()?.toDomain(),
                            girlLatestMedical = girlMedical.firstOrNull()?.toDomain(),
                            todayMemos = todayMemosByChild,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private data class DataTuple(
        val children: List<ChildProfile>,
        val boyToilet: List<ToiletRecord>,
        val girlToilet: List<ToiletRecord>,
        val boyMedical: List<com.babymakisuk.coredata.entity.MedicalVisitEntity>,
        val girlMedical: List<com.babymakisuk.coredata.entity.MedicalVisitEntity>,
        val todayMemos: List<com.babymakisuk.coremodel.Memo>
    )
}
