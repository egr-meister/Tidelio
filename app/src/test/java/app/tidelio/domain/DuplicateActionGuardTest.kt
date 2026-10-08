package app.tidelio.domain

import app.tidelio.domain.entries.DuplicateActionGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateActionGuardTest {
    private var clock = 1_000L
    private val guard = DuplicateActionGuard(windowMillis = 800L) { clock }

    @Test
    fun rapidRepeatOfSameActionIsIgnored() {
        assertTrue(guard.tryAcquire(250))
        clock += 100
        assertFalse(guard.tryAcquire(250))
        clock += 699
        assertFalse(guard.tryAcquire(250))
    }

    @Test
    fun sameActionAfterWindowIsAccepted() {
        assertTrue(guard.tryAcquire(250))
        clock += 800
        assertTrue(guard.tryAcquire(250))
    }

    @Test
    fun differentActionsAreIndependent() {
        assertTrue(guard.tryAcquire(250))
        clock += 50
        assertTrue(guard.tryAcquire(500))
    }

    @Test
    fun firstActionAtTimeZeroIsAccepted() {
        clock = 0
        assertTrue(guard.tryAcquire(150))
    }
}
