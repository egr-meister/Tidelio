package app.tidelio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tidelio.AppContainer
import app.tidelio.data.repository.UserPrefs
import app.tidelio.domain.goals.GoalApplyFrom
import app.tidelio.domain.goals.GoalChange
import app.tidelio.domain.goals.effectiveDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SettingsUiState(
    val loaded: Boolean = false,
    val today: LocalDate,
    val todayGoal: Int? = null,
    val upcomingGoal: GoalChange? = null,
    val presets: List<Int> = UserPrefs.DEFAULT_PRESETS,
    val reducedMotion: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        container.preferences.prefs,
        container.goals.timeline,
        container.todayProvider.today,
    ) { prefs, goals, today ->
        SettingsUiState(
            loaded = true,
            today = today,
            todayGoal = goals.goalFor(today),
            upcomingGoal = goals.nextChangeAfter(today),
            presets = prefs.presets,
            reducedMotion = prefs.reducedMotion,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(today = container.todayProvider.today.value),
    )

    fun setGoal(goalMl: Int, applyFrom: GoalApplyFrom) {
        viewModelScope.launch {
            container.goals.setGoal(goalMl, applyFrom.effectiveDate(container.todayProvider.today.value))
        }
    }

    fun setPresets(values: List<Int>) {
        viewModelScope.launch { container.preferences.setPresets(values) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setReducedMotion(enabled) }
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            container.clearAllData()
            onDone()
        }
    }
}
