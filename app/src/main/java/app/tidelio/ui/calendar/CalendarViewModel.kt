package app.tidelio.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tidelio.AppContainer
import app.tidelio.domain.time.monthGrid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

enum class DayStatus { RECORDED, NO_RECORD, FUTURE }

data class CalendarCell(
    val date: LocalDate,
    val status: DayStatus,
    val totalMl: Long?,
    val goalMl: Int?,
    val isToday: Boolean,
    val isSelected: Boolean,
) {
    val goalMet: Boolean get() = totalMl != null && goalMl != null && totalMl >= goalMl
}

data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    val firstDayOfWeek: DayOfWeek,
    val cells: List<CalendarCell?>,
    val selected: LocalDate?,
    val maxMonthTotal: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val container: AppContainer,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private val monthFlow = savedState.getStateFlow(KEY_MONTH, YearMonth.from(container.todayProvider.today.value).toString())
        .map { YearMonth.parse(it) }

    private val selectedFlow = savedState.getStateFlow(KEY_SELECTED, Long.MIN_VALUE)
        .map { if (it == Long.MIN_VALUE) null else LocalDate.ofEpochDay(it) }

    val state: StateFlow<CalendarUiState> = monthFlow.flatMapLatest { month ->
        combine(
            container.entries.observeDailyTotals(month.atDay(1), month.atEndOfMonth()),
            container.goals.timeline,
            container.todayProvider.today,
            selectedFlow,
        ) { totals, goals, today, selected ->
            val byDate = totals.associateBy { it.date }
            val cells = monthGrid(month, firstDayOfWeek).map { date ->
                date?.let {
                    val total = byDate[it]?.totalMl
                    CalendarCell(
                        date = it,
                        status = when {
                            it.isAfter(today) -> DayStatus.FUTURE
                            total != null -> DayStatus.RECORDED
                            else -> DayStatus.NO_RECORD
                        },
                        totalMl = total,
                        goalMl = goals.goalFor(it),
                        isToday = it == today,
                        isSelected = it == selected,
                    )
                }
            }
            CalendarUiState(
                month = month,
                today = today,
                firstDayOfWeek = firstDayOfWeek,
                cells = cells,
                selected = selected,
                maxMonthTotal = totals.maxOfOrNull { it.totalMl } ?: 0L,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(
            month = YearMonth.from(container.todayProvider.today.value),
            today = container.todayProvider.today.value,
            firstDayOfWeek = firstDayOfWeek,
            cells = emptyList(),
            selected = null,
            maxMonthTotal = 0,
        ),
    )

    fun showMonth(month: YearMonth) {
        savedState[KEY_MONTH] = month.toString()
    }

    fun select(date: LocalDate) {
        savedState[KEY_SELECTED] = date.toEpochDay()
    }

    private companion object {
        const val KEY_MONTH = "month"
        const val KEY_SELECTED = "selected"
    }
}
