package app.tidelio.domain.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Publishes the device's local "today".
 *
 * - [refresh] is called on launch and every time the app returns to the foreground,
 *   which also picks up manual clock or time-zone changes.
 * - While the process is alive, a single suspended timer fires shortly after the next
 *   local midnight so open screens switch to the new date. This is not polling: it sleeps
 *   until midnight and is recomputed after each firing.
 */
class TodayProvider(
    private val time: TimeSource,
    scope: CoroutineScope,
) {
    private val _today = MutableStateFlow(time.today())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    init {
        scope.launch {
            while (true) {
                delay(millisUntilNextMidnight(time.now(), time.zone()) + MIDNIGHT_GRACE_MS)
                refresh()
            }
        }
    }

    fun refresh() {
        _today.value = time.today()
    }

    private companion object {
        const val MIDNIGHT_GRACE_MS = 250L
    }
}
