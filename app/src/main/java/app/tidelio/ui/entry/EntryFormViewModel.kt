package app.tidelio.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tidelio.AppContainer
import app.tidelio.domain.entries.EntryInputErrors
import app.tidelio.domain.entries.EntryValidation
import app.tidelio.domain.entries.EntryValidator
import app.tidelio.domain.entries.WaterEntry
import app.tidelio.domain.time.localNow
import app.tidelio.ui.common.Fmt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class EntryFormState(
    val isEdit: Boolean,
    val loading: Boolean,
    val notFound: Boolean,
    val amountText: String,
    val date: LocalDate,
    val time: LocalTime,
    val today: LocalDate,
    val errors: EntryInputErrors,
    val dirty: Boolean,
    val saving: Boolean,
)

/**
 * Add / edit form. The draft lives in [SavedStateHandle], so it survives rotation,
 * configuration changes and process recreation.
 */
class EntryFormViewModel(
    private val container: AppContainer,
    private val savedState: SavedStateHandle,
    private val entryId: Long?,
    presetDate: LocalDate?,
) : ViewModel() {

    private val amount = savedState.getStateFlow(KEY_AMOUNT, "")
    private val dateEpoch = savedState.getStateFlow(KEY_DATE, -1L)
    private val timeSecond = savedState.getStateFlow(KEY_TIME, -1)
    private val initialized = savedState.getStateFlow(KEY_INIT, false)
    private val notFound = MutableStateFlow(false)
    private val errors = MutableStateFlow(EntryInputErrors())
    private val saving = MutableStateFlow(false)
    private var original: WaterEntry? = null

    private val events = Channel<Unit>(Channel.BUFFERED)

    /** Emits once when the form should close (saved or deleted). */
    val finished: Flow<Unit> = events.receiveAsFlow()

    init {
        if (entryId != null) {
            viewModelScope.launch {
                val entry = container.entries.get(entryId)
                if (entry == null) {
                    notFound.value = true
                    return@launch
                }
                original = entry
                if (!savedState.get<Boolean>(KEY_INIT).orFalse()) {
                    setInitial(entry.amountMl.toString(), entry.localDate, entry.localTime)
                }
            }
        } else if (!savedState.get<Boolean>(KEY_INIT).orFalse()) {
            val now = container.time.localNow()
            val date = presetDate?.takeIf { !it.isAfter(now.toLocalDate()) } ?: now.toLocalDate()
            val time = if (date == now.toLocalDate()) now.toLocalTime().withSecond(0).withNano(0) else LocalTime.NOON
            setInitial("", date, time)
        }
    }

    private fun setInitial(amountText: String, date: LocalDate, time: LocalTime) {
        savedState[KEY_AMOUNT] = amountText
        savedState[KEY_DATE] = date.toEpochDay()
        savedState[KEY_TIME] = time.toSecondOfDay()
        savedState[KEY_INIT_AMOUNT] = amountText
        savedState[KEY_INIT_DATE] = date.toEpochDay()
        savedState[KEY_INIT_TIME] = time.toSecondOfDay()
        savedState[KEY_INIT] = true
    }

    private data class Draft(val amount: String, val date: Long, val time: Int, val init: Boolean)

    private val draft = combine(amount, dateEpoch, timeSecond, initialized) { a, d, t, i -> Draft(a, d, t, i) }

    val state: StateFlow<EntryFormState> = combine(
        draft,
        container.todayProvider.today,
        errors,
        saving,
        notFound,
    ) { d, today, err, isSaving, missing ->
        val date = if (d.date >= 0) LocalDate.ofEpochDay(d.date) else today
        val time = if (d.time >= 0) LocalTime.ofSecondOfDay(d.time.toLong()) else LocalTime.NOON
        EntryFormState(
            isEdit = entryId != null,
            loading = !d.init && !missing,
            notFound = missing,
            amountText = d.amount,
            date = date,
            time = time,
            today = today,
            errors = err,
            dirty = d.init && isDirty(d),
            saving = isSaving,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EntryFormState(
            isEdit = entryId != null,
            loading = true,
            notFound = false,
            amountText = "",
            date = container.todayProvider.today.value,
            time = LocalTime.NOON,
            today = container.todayProvider.today.value,
            errors = EntryInputErrors(),
            dirty = false,
            saving = false,
        ),
    )

    private fun isDirty(d: Draft): Boolean =
        d.amount != savedState.get<String>(KEY_INIT_AMOUNT) ||
            d.date != savedState.get<Long>(KEY_INIT_DATE) ||
            d.time != savedState.get<Int>(KEY_INIT_TIME)

    fun onAmountChange(text: String) {
        savedState[KEY_AMOUNT] = text
        errors.value = errors.value.copy(amount = null)
    }

    fun onDateChange(date: LocalDate) {
        savedState[KEY_DATE] = date.toEpochDay()
        errors.value = errors.value.copy(date = null, time = null)
    }

    fun onTimeChange(time: LocalTime) {
        savedState[KEY_TIME] = time.withNano(0).toSecondOfDay()
        errors.value = errors.value.copy(time = null)
    }

    fun save() {
        if (saving.value) return
        val s = state.value
        val existing = original
        val unchangedMoment = existing != null && existing.localDate == s.date && existing.localTime == s.time
        // Keep the entry's own zone when its local date/time is untouched (e.g. only amount edited).
        val zone = if (unchangedMoment) ZoneId.of(existing!!.zoneId) else container.time.zone()
        val result = EntryValidator.validate(s.amountText, s.date, s.time, zone, container.time.now())
        when (result) {
            is EntryValidation.Invalid -> errors.value = result.errors
            is EntryValidation.Valid -> {
                saving.value = true
                viewModelScope.launch {
                    try {
                        if (existing != null) {
                            container.editor.edit(existing, result.amountMl, result.dateTime)
                        } else {
                            val created = container.editor.add(result.amountMl, result.dateTime)
                            container.undo.offer(
                                "Added ${Fmt.ml(created.amountMl)} on ${Fmt.dateShort(created.localDate)} at ${Fmt.time(created.localTime)}",
                            ) { container.editor.delete(created.id) }
                        }
                        events.send(Unit)
                    } finally {
                        saving.value = false
                    }
                }
            }
        }
    }

    fun delete() {
        val id = entryId ?: return
        viewModelScope.launch {
            val removed = container.editor.delete(id)
            if (removed != null) {
                container.undo.offer("Deleted ${Fmt.ml(removed.amountMl)} at ${Fmt.time(removed.localTime)}") {
                    container.editor.restore(removed)
                }
            }
            events.send(Unit)
        }
    }

    private fun Boolean?.orFalse(): Boolean = this ?: false

    private companion object {
        const val KEY_AMOUNT = "draft_amount"
        const val KEY_DATE = "draft_date"
        const val KEY_TIME = "draft_time"
        const val KEY_INIT = "draft_initialized"
        const val KEY_INIT_AMOUNT = "initial_amount"
        const val KEY_INIT_DATE = "initial_date"
        const val KEY_INIT_TIME = "initial_time"
    }
}
