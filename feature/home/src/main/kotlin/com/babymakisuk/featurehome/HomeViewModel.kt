package com.babymakisuk.featurehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymakisuk.coredata.repository.MedicalRepository
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coredata.repository.GrowthRepository
import com.babymakisuk.coredata.repository.MemoRepository
import com.babymakisuk.coredata.repository.MonthlyReportRepository
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
    private val medicalRepo: MedicalRepository,
    private val systemReminderRepository: SystemReminderRepository,
    private val monthlyReportRepository: MonthlyReportRepository,
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

    fun resolveReminder(id: String) {
        viewModelScope.launch {
            systemReminderRepository.markResolved(id)
        }
    }

    fun logToilet(childId: Long) {
        viewModelScope.launch {
            // 1. 記錄前先檢查：若目前已超過閾值，先產生一筆「異常間隔」的提醒紀錄
            checkLongNoBm(childId)
            
            // 2. 執行本次排便紀錄
            toiletRepository.insertToilet(ToiletRecord(childId = childId))
            
            // 3. 記錄後處理：因為寶寶排便了，將所有未處理的「長時未排便」提醒標記為已解決
            systemReminderRepository.markAllResolvedByType(
                childId, 
                com.babymakisuk.coremodel.SystemReminderType.LONG_NO_BM,
            )
        }
    }

    private suspend fun checkLongNoBm(childId: Long) {
        val latestTime = toiletRepository.getLatestToiletTime(childId) ?: return
        val hoursSince = ((System.currentTimeMillis() - latestTime) / 3600000).toInt()
        if (hoursSince >= 48) {
            // 檢查是否已有「未處理」的同類型提醒，避免重複產生
            val existing = systemReminderRepository.getUnresolvedByType(
                childId, 
                com.babymakisuk.coremodel.SystemReminderType.LONG_NO_BM,
            ).first()
            
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
                .onEach { children ->
                    // 每次孩童清單更新或啟動時，主動巡檢排便間隔與月報提醒
                    children.forEach { checkLongNoBm(it.id) }
                    monthlyReportRepository.checkAndCreateMonthlyReportReminder()
                }
                .flatMapLatest { children ->
                    val boy = children.firstOrNull { it.gender == Gender.MALE }
                    val girl = children.firstOrNull { it.gender == Gender.FEMALE }

                    val boyToiletFlow = boy?.let { toiletRepository.getToiletRecords(it.id) }
                        ?: flowOf(emptyList())
                    val girlToiletFlow = girl?.let { toiletRepository.getToiletRecords(it.id) }
                        ?: flowOf(emptyList())

                    val boyMedicalFlow = boy?.let { medicalRepo.observeByChild(it.id) }
                        ?: flowOf(emptyList())
                    val girlMedicalFlow = girl?.let { medicalRepo.observeByChild(it.id) }
                        ?: flowOf(emptyList())

                    val todayDate = LocalDate.now().toEpochDay()
                    val todayMemosFlow = flowOf(memoRepository.getByDate(todayDate))

                    val remindersFlow = if (children.isNotEmpty()) {
                        // 觀察所有孩子的提醒
                        val flows = children.map { systemReminderRepository.getByChildId(it.id) }
                        combine(flows) { arrays -> arrays.flatMap { it } }
                    } else {
                        flowOf(emptyList())
                    }

                    combine(
                        combine(
                            boyToiletFlow,
                            girlToiletFlow,
                            boyMedicalFlow,
                            girlMedicalFlow,
                            todayMemosFlow,
                            ::HomeCoreSnapshot
                        ),
                        remindersFlow
                    ) { core, reminders ->
                        DataTuple(
                            children = children,
                            boyToilet = core.boyToilet,
                            girlToilet = core.girlToilet,
                            boyMedical = core.boyMedical,
                            girlMedical = core.girlMedical,
                            todayMemos = core.todayMemos,
                            reminders = reminders
                        )
                    }
                }
                .collect { tuple ->
                    val children = tuple.children
                    val boyToilet = tuple.boyToilet
                    val girlToilet = tuple.girlToilet
                    val boyMedical = tuple.boyMedical
                    val girlMedical = tuple.girlMedical
                    val todayMemos = tuple.todayMemos
                    val reminders = tuple.reminders
                    
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
                            boyLatestMedical = boyMedical.firstOrNull(),
                            girlLatestMedical = girlMedical.firstOrNull(),
                            todayMemos = todayMemosByChild,
                            systemReminders = reminders,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private data class HomeCoreSnapshot(
        val boyToilet: List<ToiletRecord>,
        val girlToilet: List<ToiletRecord>,
        val boyMedical: List<com.babymakisuk.coremodel.MedicalVisit>,
        val girlMedical: List<com.babymakisuk.coremodel.MedicalVisit>,
        val todayMemos: List<com.babymakisuk.coremodel.Memo>
    )

    private data class DataTuple(
        val children: List<ChildProfile>,
        val boyToilet: List<ToiletRecord>,
        val girlToilet: List<ToiletRecord>,
        val boyMedical: List<com.babymakisuk.coremodel.MedicalVisit>,
        val girlMedical: List<com.babymakisuk.coremodel.MedicalVisit>,
        val todayMemos: List<com.babymakisuk.coremodel.Memo>,
        val reminders: List<com.babymakisuk.coremodel.SystemReminder>
    )
}
