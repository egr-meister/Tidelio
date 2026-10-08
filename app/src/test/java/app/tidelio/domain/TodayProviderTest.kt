package app.tidelio.domain

import app.tidelio.domain.time.TodayProvider
import app.tidelio.testing.FakeTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TodayProviderTest {

    @Test
    fun switchesToNewDateAfterMidnightWhileOpen() {
        val zone = ZoneId.of("Europe/Minsk")
        val time = FakeTime.at(LocalDateTime.of(2026, 10, 7, 23, 59, 0), zone)
        val scope = TestScope(StandardTestDispatcher())
        val provider = TodayProvider(time, scope)
        scope.runCurrent()
        assertEquals(LocalDate.of(2026, 10, 7), provider.today.value)

        // Virtual time passes 61 s; the fake clock follows.
        time.instant = time.instant.plusSeconds(61)
        scope.advanceTimeBy(61_000)
        scope.runCurrent()
        assertEquals(LocalDate.of(2026, 10, 8), provider.today.value)
        scope.cancel()
    }

    @Test
    fun refreshPicksUpTimeZoneChange() {
        val time = FakeTime.at(LocalDateTime.of(2026, 10, 7, 22, 30), ZoneId.of("Europe/Minsk"))
        val scope = TestScope(StandardTestDispatcher())
        val provider = TodayProvider(time, scope)
        assertEquals(LocalDate.of(2026, 10, 7), provider.today.value)
        time.zoneId = ZoneId.of("Asia/Tokyo")
        provider.refresh()
        assertEquals(LocalDate.of(2026, 10, 8), provider.today.value)
        scope.cancel()
    }
}
