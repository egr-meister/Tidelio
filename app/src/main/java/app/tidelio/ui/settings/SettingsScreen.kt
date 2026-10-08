@file:OptIn(ExperimentalMaterial3Api::class)

package app.tidelio.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tidelio.domain.entries.AmountParse
import app.tidelio.domain.entries.EntryLimits
import app.tidelio.domain.entries.parseWholeMl
import app.tidelio.ui.common.ConfirmDialog
import app.tidelio.ui.common.ContentWidth
import app.tidelio.ui.common.Fmt
import app.tidelio.ui.common.MlTextField

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenPrivacy: () -> Unit,
    onDataCleared: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showGoal by rememberSaveable { mutableStateOf(false) }
    var showPresets by rememberSaveable { mutableStateOf(false) }
    var showClear by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        ContentWidth(Modifier.padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SectionTitle("Goal")
                SettingRow(
                    title = "Daily goal",
                    value = buildString {
                        append(state.todayGoal?.let { Fmt.ml(it) } ?: "Not set")
                        state.upcomingGoal?.let { append(" · ${Fmt.ml(it.goalMl)} from ${Fmt.dateMediumYear(it.effectiveDate)}") }
                    },
                    description = "You choose this goal. Changes never rewrite earlier dates.",
                    onClick = { showGoal = true },
                )
                HorizontalDivider()

                SectionTitle("Logging")
                SettingRow(
                    title = "Quick-add amounts",
                    value = state.presets.joinToString(" · ") { Fmt.ml(it) },
                    description = "The four amounts shown on the Today screen.",
                    onClick = { showPresets = true },
                )
                SettingRow(
                    title = "Units",
                    value = "Milliliters (mL)",
                    description = "All amounts are recorded in mL.",
                    onClick = null,
                )
                HorizontalDivider()

                SectionTitle("Display")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(
                            value = state.reducedMotion,
                            onValueChange = viewModel::setReducedMotion,
                            role = Role.Switch,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Reduce wave animation", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Turns off the ripple after adding water. The system “Remove animations” setting is also respected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = state.reducedMotion, onCheckedChange = null)
                }
                HorizontalDivider()

                SectionTitle("Privacy and data")
                SettingRow(
                    title = "Privacy information",
                    value = "Entries and goals stay on this device.",
                    description = null,
                    onClick = onOpenPrivacy,
                )
                SettingRow(
                    title = "Clear all local data",
                    value = "Remove entries, goals and preferences",
                    description = null,
                    onClick = { showClear = true },
                    titleColor = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "Tidelio 1.0.0 · Works fully offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showGoal) {
        GoalEditDialog(
            currentGoal = state.todayGoal,
            onDismiss = { showGoal = false },
            onSave = { ml, from ->
                showGoal = false
                viewModel.setGoal(ml, from)
            },
        )
    }
    if (showPresets) {
        PresetsDialog(
            current = state.presets,
            onDismiss = { showPresets = false },
            onSave = {
                showPresets = false
                viewModel.setPresets(it)
            },
        )
    }
    if (showClear) {
        ConfirmDialog(
            title = "Clear all local data?",
            text = "This permanently removes all water entries, goal history and preferences from this device. " +
                "This cannot be undone. Tidelio will return to initial setup.",
            confirmLabel = "Clear all data",
            onConfirm = {
                showClear = false
                viewModel.clearAllData(onDataCleared)
            },
            onDismiss = { showClear = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    description: String?,
    onClick: (() -> Unit)?,
    titleColor: Color = Color.Unspecified,
) {
    val base = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
    Column(
        (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PresetsDialog(current: List<Int>, onDismiss: () -> Unit, onSave: (List<Int>) -> Unit) {
    var p1 by rememberSaveable { mutableStateOf(current.getOrNull(0)?.toString() ?: "") }
    var p2 by rememberSaveable { mutableStateOf(current.getOrNull(1)?.toString() ?: "") }
    var p3 by rememberSaveable { mutableStateOf(current.getOrNull(2)?.toString() ?: "") }
    var p4 by rememberSaveable { mutableStateOf(current.getOrNull(3)?.toString() ?: "") }
    var errors by rememberSaveable { mutableStateOf(listOf<String?>(null, null, null, null)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick-add amounts") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Each amount: ${Fmt.ml(EntryLimits.MIN_ML)}–${Fmt.ml(EntryLimits.MAX_ML)}, whole milliliters.",
                    style = MaterialTheme.typography.bodySmall,
                )
                MlTextField(p1, { p1 = it }, "Amount 1", errors[0], imeAction = ImeAction.Next)
                MlTextField(p2, { p2 = it }, "Amount 2", errors[1], imeAction = ImeAction.Next)
                MlTextField(p3, { p3 = it }, "Amount 3", errors[2], imeAction = ImeAction.Next)
                MlTextField(p4, { p4 = it }, "Amount 4", errors[3])
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = listOf(p1, p2, p3, p4).map { parseWholeMl(it, EntryLimits.MIN_ML, EntryLimits.MAX_ML) }
                errors = parsed.map { (it as? AmountParse.Invalid)?.message }
                if (parsed.all { it is AmountParse.Valid }) {
                    onSave(parsed.map { (it as AmountParse.Valid).ml })
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
