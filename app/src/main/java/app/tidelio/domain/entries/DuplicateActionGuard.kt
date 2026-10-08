package app.tidelio.domain.entries

/**
 * Rejects a repeat of the same action key within [windowMillis]. Prevents duplicate entries
 * caused by rapid double-taps on the same quick-add control. Different keys are independent
 * of the window, so tapping 250 mL and then 500 mL quickly records both.
 */
class DuplicateActionGuard(
    private val windowMillis: Long = 800L,
    private val clock: () -> Long,
) {
    private var lastKey: Any? = null
    private var lastAt: Long? = null

    @Synchronized
    fun tryAcquire(key: Any): Boolean {
        val now = clock()
        val previous = lastAt
        if (previous != null && key == lastKey && now - previous in 0 until windowMillis) {
            return false
        }
        lastKey = key
        lastAt = now
        return true
    }
}
