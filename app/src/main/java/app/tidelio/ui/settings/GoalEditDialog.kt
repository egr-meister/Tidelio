package app.tidelio.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.tidelio.domain.entries.AmountParse
import app.tidelio.domain.goals.GoalApplyFrom
import app.tidelio.domain.goals.GoalLimits
import app.tidelio.domain.goals.parseGoal
import app.tidelio.ui.common.Fmt
import app.tidelio.ui.common.MlTextField

/**
 * Edits the daily goal and asks whether it applies starting today or tomorrow.
 * Earlier dates always keep the goals that were in effect for them.
 */
@Composable
fun GoalEditDialog(
    currentGoal: Int?,
    onDismiss: () -> Unit,
    onSave: (Int, GoalApplyFrom) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentGoal?.toString() ?: "") }
    var applyFrom by rememberSaveable { mutableStateOf(GoalApplyFrom.TODAY) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentGoal == null) "Set a daily goal" else "Change daily goal") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "You choose this goal. Tidelio does not calculate hydration needs or give health advice. " +
                        "Allowed range: ${Fmt.ml(GoalLimits.MIN_ML)}–${Fmt.ml(GoalLimits.MAX_ML)}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                MlTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    label = "Daily goal",
                    error = error,
                )
                Spacer(Modifier.height(8.dp))
                Text("Apply the new goal", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.selectableGroup()) {
                    GoalApplyFrom.entries.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = applyFrom == option,
                                    onClick = { applyFrom = option },
                                    role = Role.RadioButton,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = applyFrom == option, onClick = null)
                            Text(
                                text = when (option) {
                                    GoalApplyFrom.TODAY -> "Starting today"
                                    GoalApplyFrom.TOMORROW -> "Starting tomorrow"
                                },
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                Text(
                    "Earlier dates keep the goals that applied to them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (val parsed = parseGoal(text)) {
                    is AmountParse.Valid -> onSave(parsed.ml, applyFrom)
                    is AmountParse.Invalid -> error = parsed.message
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
