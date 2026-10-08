package app.tidelio.ui.wave

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tidelio.AppContainer
import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.entries.PeriodSummaries
import app.tidelio.domain.entries.PeriodSummary
import app.tidelio.domain.entries.WaterEntry
import app.tidelio.domain.statistics.WaveCalculator
import app.tidelio.domain.statistics.WaveModel
import app.tidelio.ui.common.Fmt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayUiState(
    val loaded: Boolean = false,
    val date: LocalDate,
    val today: LocalDate,
    val entries: List<WaterEntry> = emptyList(),
    val wave: WaveModel = WaveCalculator.build(emptyList(), null),
    val periods: List<PeriodSummary> = PeriodSummaries.of(emptyList()),
    val selectedPeriod: DayPeriod? = null,
    val presets: List<Int> = emptyList(),
    val reducedMotion: Boolean = false,
) {
    val isToday: Boolean get() = date == today
    val isFuture: Boolean get() = date.isAfter(today)
    val visibleEntries: List<WaterEntry>
        get() = if (selectedPeriod == null) entries else entries.filter { it.period == selectedPeriod }
}

/**
 * Backs both the Today screen ([fixedDate] = null, follows the device's local date including
 * midnight rollover) and calendar day details ([fixedDate] set).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val container: AppContainer,
    private val savedState: SavedStateHandle,
    private val fixedDate: LocalDate?,
) : ViewModel() {

    private val selectedPeriod: StateFlow<DayPeriod?> = savedState.getStateFlow<String?>(KEY_PERIOD, null)
        .map { name -> name?.let { runCatching { DayPeriod.valueOf(it) }.getOrNull() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val dateFlow: Flow<LocalDate> =
        if (fixedDate != null) flowOf(fixedDate) else container.todayProvider.today

    val state: StateFlow<DayUiState> = dateFlow.distinctUntilChanged().flatMapLatest { date ->
        combine(
            container.entries.observeDay(date),
            container.goals.timeline,
            container.preferences.prefs,
            container.todayProvider.today,
            selectedPeriod,
        ) { entries, goals, prefs, today, period ->
            val goal = goals.goalFor(date)
            DayUiState(
                loaded = true,
                date = date,
                today = today,
                entries = entries,
                wave = WaveCalculator.build(entries, goal),
                periods = PeriodSummaries.of(entries),
                selectedPeriod = period,
                presets = prefs.presets,
                reducedMotion = prefs.reducedMotion,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DayUiState(date = fixedDate ?: container.todayProvider.today.value, today = container.todayProvider.today.value),
    )

    fun selectPeriod(period: DayPeriod?) {
        val current = selectedPeriod.value
        savedState[KEY_PERIOD] = if (period == null || period == current) null else period.name
    }

    /** Quick add at the current local date and time, with duplicate-tap protection and Undo. */
    fun quickAdd(amountMl: Int) {
        if (!container.quickAddGuard.tryAcquire(amountMl)) return
        viewModelScope.launch {
            val entry = container.editor.quickAdd(amountMl)
            container.undo.offer("Added ${Fmt.ml(amountMl)} at ${Fmt.time(entry.localTime)}") {
                container.editor.delete(entry.id)
            }
        }
    }

    private companion object {
        const val KEY_PERIOD = "selected_period"
    }
}
