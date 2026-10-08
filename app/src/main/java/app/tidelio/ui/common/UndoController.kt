package app.tidelio.ui.common

import app.tidelio.domain.entries.OneShotUndo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/** A short confirmation with an Undo action that can be applied once. */
class UndoMessage(
    val id: Long,
    val text: String,
    val undo: OneShotUndo,
)

/**
 * App-level confirmations so an Undo survives navigation (e.g. delete in Edit, then return
 * to the day screen). Shown by the root scaffold's snackbar host.
 */
class UndoController(private val scope: CoroutineScope) {
    private val ids = AtomicLong(0)
    private val _messages = MutableSharedFlow<UndoMessage>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<UndoMessage> = _messages.asSharedFlow()

    @Volatile
    private var latest: UndoMessage? = null

    fun offer(text: String, undo: suspend () -> Unit) {
        val message = UndoMessage(ids.incrementAndGet(), text, OneShotUndo(undo))
        latest = message
        _messages.tryEmit(message)
    }

    fun performUndo(message: UndoMessage) {
        scope.launch { message.undo.run() }
    }

    /** Invalidates pending undo actions (used by "Clear all data"). */
    fun clear() {
        latest?.undo?.disarm()
        latest = null
    }
}
