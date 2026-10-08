package app.tidelio.ui.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tidelio.AppContainer
import app.tidelio.domain.statistics.StatisticsCalculator
import app.tidelio.domain.statistics.StatisticsResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatisticsUiState(
    val loaded: Boolean,
    val rangeDays: Int,
    val result: StatisticsResult?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val container: AppContainer,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val range = savedState.getStateFlow(KEY_RANGE, 7)

    val state: StateFlow<StatisticsUiState> =
        combine(range, container.todayProvider.today) { days, today -> days to today }
            .flatMapLatest { (days, today) ->
                val start = today.minusDays((days - 1).toLong())
                combine(container.entries.observeRange(start, today), container.goals.timeline) { entries, goals ->
                    StatisticsUiState(
                        loaded = true,
                        rangeDays = days,
                        result = StatisticsCalculator.compute(today, days, entries, goals),
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState(false, 7, null))

    val rangeDays: StateFlow<Int> = range.map { it }.stateIn(viewModelScope, SharingStarted.Eagerly, 7)

    fun setRange(days: Int) {
        if (days == 7 || days == 30) savedState[KEY_RANGE] = days
    }

    private companion object {
        const val KEY_RANGE = "range_days"
    }
}
