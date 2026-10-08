package app.tidelio.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tidelio.domain.entries.AmountParse
import app.tidelio.domain.goals.GoalLimits
import app.tidelio.domain.goals.parseGoal
import app.tidelio.ui.common.ContentWidth
import app.tidelio.ui.common.Fmt
import app.tidelio.ui.common.MlTextField

private const val CUSTOM = -1

/**
 * First-launch goal choice. Examples are editable and explicitly not recommendations.
 * No age, weight, sex, medical information or account is requested.
 */
@Composable
fun SetupScreen(
    onGoalChosen: (Int) -> Unit,
    onSkip: () -> Unit,
) {
    var choice by rememberSaveable { mutableStateOf(2_000) }
    var customText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        ContentWidth(Modifier.safeDrawingPadding().imePadding()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Tidelio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Choose a daily goal",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Pick one of these selectable examples or enter your own amount. They are examples only — " +
                        "not personalized recommendations. You can change the goal at any time in Settings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (GoalLimits.EXAMPLES + CUSTOM).forEach { option ->
                        val selected = choice == option
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = selected,
                                    onClick = {
                                        choice = option
                                        error = null
                                    },
                                    role = Role.RadioButton,
                                ),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selected, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    if (option == CUSTOM) "Custom amount" else "${Fmt.ml(option)} (example)",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
                if (choice == CUSTOM) {
                    MlTextField(
                        value = customText,
                        onValueChange = {
                            customText = it
                            error = null
                        },
                        label = "Custom daily goal (${Fmt.number(GoalLimits.MIN_ML)}–${Fmt.number(GoalLimits.MAX_ML)})",
                        error = error,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (choice == CUSTOM) {
                            when (val parsed = parseGoal(customText)) {
                                is AmountParse.Valid -> onGoalChosen(parsed.ml)
                                is AmountParse.Invalid -> error = parsed.message
                            }
                        } else {
                            onGoalChosen(choice)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("Set goal") }
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Skip for now — I'll log water first")
                }
                Text(
                    "Tidelio records the amounts you enter. It does not calculate medical hydration requirements " +
                        "or provide health advice. Everything stays on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
