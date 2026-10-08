@file:OptIn(ExperimentalMaterial3Api::class)

package app.tidelio.ui.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tidelio.R
import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.entries.EntryLimits
import app.tidelio.ui.common.ConfirmDialog
import app.tidelio.ui.common.ContentWidth
import app.tidelio.ui.common.Fmt
import app.tidelio.ui.common.MlTextField
import java.time.LocalDate
import java.time.LocalTime

private const val MILLIS_PER_DAY = 86_400_000L

@Composable
fun EntryFormScreen(viewModel: EntryFormViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.finished.collect { onClose() }
    }

    // Predictive-back compatible: only intercepts Back while there are unsaved changes.
    BackHandler(enabled = state.dirty) { showDiscard = true }
    val requestClose: () -> Unit = {
        if (state.dirty) {
            showDiscard = true
        } else {
            onClose()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit entry" else "Add water") },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        ContentWidth(Modifier.padding(padding).imePadding()) {
            when {
                state.notFound -> Column(Modifier.padding(16.dp)) {
                    Text("This entry no longer exists.", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onClose) { Text("Close") }
                }
                state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MlTextField(
                        value = state.amountText,
                        onValueChange = viewModel::onAmountChange,
                        label = "Amount (${Fmt.number(EntryLimits.MIN_ML)}–${Fmt.number(EntryLimits.MAX_ML)})",
                        error = state.errors.amount,
                        imeAction = ImeAction.Done,
                    )
                    PickerField(
                        label = "Date",
                        value = Fmt.dateLong(state.date),
                        error = state.errors.date,
                        onClick = { showDate = true },
                    )
                    PickerField(
                        label = "Time",
                        value = Fmt.time(state.time),
                        error = state.errors.time,
                        onClick = { showTime = true },
                    )
                    val period = DayPeriod.of(state.time)
                    Text(
                        "Period: ${period.label} (${period.rangeLabel})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = requestClose,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) { Text("Cancel") }
                        Button(
                            onClick = viewModel::save,
                            enabled = !state.saving,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) { Text("Save") }
                    }
                    if (state.isEdit) {
                        TextButton(
                            onClick = { showDelete = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                            Text("Delete entry", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            text = "Your unsaved changes to this entry will be lost.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onConfirm = {
                showDiscard = false
                onClose()
            },
            onDismiss = { showDiscard = false },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "Delete this entry?",
            text = "${state.amountText.ifBlank { "This" }} mL at ${Fmt.time(state.time)} will be removed. You can undo right after.",
            confirmLabel = "Delete",
            onConfirm = {
                showDelete = false
                viewModel.delete()
            },
            onDismiss = { showDelete = false },
        )
    }
    if (showDate) {
        EntryDatePicker(
            initial = state.date,
            today = state.today,
            onDismiss = { showDate = false },
            onPick = {
                showDate = false
                viewModel.onDateChange(it)
            },
        )
    }
    if (showTime) {
        EntryTimePicker(
            initial = state.time,
            onDismiss = { showTime = false },
            onPick = {
                showTime = false
                viewModel.onTimeChange(it)
            },
        )
    }
}

@Composable
private fun PickerField(label: String, value: String, error: String?, onClick: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .semantics { contentDescription = "$label: $value. Change $label" },
        ) {
            Text(value, modifier = Modifier.fillMaxWidth())
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EntryDatePicker(initial: LocalDate, today: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val todayEpoch = today.toEpochDay()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * MILLIS_PER_DAY,
        initialDisplayedMonthMillis = initial.toEpochDay() * MILLIS_PER_DAY,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                Math.floorDiv(utcTimeMillis, MILLIS_PER_DAY) <= todayEpoch

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(Math.floorDiv(it, MILLIS_PER_DAY))) }
                        ?: onDismiss()
                },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun EntryTimePicker(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    val compact = LocalConfiguration.current.screenHeightDp < 520
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Time") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (compact) TimeInput(state = pickerState) else TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(pickerState.hour, pickerState.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
