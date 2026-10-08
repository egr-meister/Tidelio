@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.tidelio.ui.wave

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tidelio.R
import app.tidelio.appContainer
import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.entries.PeriodSummary
import app.tidelio.domain.entries.WaterEntry
import app.tidelio.domain.goals.GoalApplyFrom
import app.tidelio.domain.statistics.WaveCalculator
import app.tidelio.domain.statistics.WaveModel
import app.tidelio.domain.time.localNow
import app.tidelio.ui.common.ContentWidth
import app.tidelio.ui.common.Fmt
import app.tidelio.ui.common.systemAnimationsDisabled
import app.tidelio.ui.settings.GoalEditDialog
import app.tidelio.ui.theme.NumberStyle
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun DayScreen(
    viewModel: DayViewModel,
    isTodayScreen: Boolean,
    onAddEntry: (LocalDate) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onSetGoal: (Int, GoalApplyFrom) -> Unit,
    onBack: (() -> Unit)?,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val container = LocalContext.current.appContainer
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

    // Restrained ripple after the day's recorded total increases.
    var rippleKey by remember { mutableIntStateOf(0) }
    var lastSeen by rememberSaveable { mutableStateOf<Pair<LocalDate, Long>?>(null) }
    LaunchedEffect(state.loaded, state.date, state.wave.totalMl) {
        if (!state.loaded) return@LaunchedEffect
        val previous = lastSeen
        if (previous != null && previous.first == state.date && state.wave.totalMl > previous.second) rippleKey++
        lastSeen = state.date to state.wave.totalMl
    }

    val nowFraction by produceState<Float?>(initialValue = null, state.isToday) {
        if (!state.isToday) {
            value = null
            return@produceState
        }
        while (true) {
            val now = container.time.localNow().toLocalTime()
            value = WaveCalculator.xOf(now)
            delay(60_000L - now.second * 1_000L)
        }
    }
    val animationsEnabled = !state.reducedMotion && !systemAnimationsDisabled()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isTodayScreen || state.isToday) "Today" else "Day details",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = Fmt.dateLong(state.date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ic_back), contentDescription = "Back to calendar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        ContentWidth(Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "summary") {
                    TotalsHeader(wave = state.wave, onSetGoal = { showGoalDialog = true }, allowGoalAction = !state.isFuture)
                }
                item(key = "wave") {
                    WaveChart(
                        model = state.wave,
                        nowFraction = if (state.isToday) nowFraction else null,
                        selectedPeriod = state.selectedPeriod,
                        rippleKey = rippleKey,
                        animationsEnabled = animationsEnabled,
                        description = waveDescription(state.date, state.isToday, state.wave, state.periods),
                    )
                }
                item(key = "actions") {
                    when {
                        state.isFuture -> Text(
                            "No records yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        state.isToday -> QuickAddRow(
                            presets = state.presets,
                            onQuickAdd = viewModel::quickAdd,
                            onCustom = { onAddEntry(state.date) },
                        )
                        else -> Button(
                            onClick = { onAddEntry(state.date) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add an entry for this day")
                        }
                    }
                }
                if (!state.isFuture) {
                    item(key = "periods") {
                        PeriodSummaryRow(
                            summaries = state.periods,
                            selected = state.selectedPeriod,
                            onSelect = viewModel::selectPeriod,
                        )
                    }
                    item(key = "listHeader") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.selectedPeriod?.let { "${it.label} entries" } ?: "Entries",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            if (state.selectedPeriod != null) {
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.selectPeriod(null) },
                                    label = { Text("All periods") },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                    }
                    val visible = state.visibleEntries
                    if (state.loaded && visible.isEmpty()) {
                        item(key = "empty") {
                            val message = when {
                                state.selectedPeriod != null && state.entries.isNotEmpty() -> "No entries in this period."
                                state.isToday -> "No water entries yet. Add your first entry."
                                else -> "No records for this day."
                            }
                            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(visible, key = { it.id }) { entry ->
                        EntryRow(entry = entry, onClick = { onOpenEntry(entry.id) })
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        GoalEditDialog(
            currentGoal = null,
            onDismiss = { showGoalDialog = false },
            onSave = { ml, from ->
                showGoalDialog = false
                onSetGoal(ml, from)
            },
        )
    }
}

@Composable
private fun TotalsHeader(wave: WaveModel, onSetGoal: () -> Unit, allowGoalAction: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        if (wave.goalMl != null) {
            Text(
                text = "${Fmt.number(wave.totalMl)} / ${Fmt.ml(wave.goalMl)} — ${wave.percent}%",
                style = NumberStyle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Recorded toward your selected daily goal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (wave.goalReached) {
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = "Your selected goal is reached.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (wave.overflowMl > 0) {
                    Text(
                        text = "${Fmt.ml(wave.overflowMl)} above your goal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Text(Fmt.ml(wave.totalMl), style = NumberStyle, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = "Recorded volume. Set a daily goal to see goal progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (allowGoalAction) {
                TextButton(onClick = onSetGoal, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Set a daily goal")
                }
            }
        }
    }
}

@Composable
private fun QuickAddRow(presets: List<Int>, onQuickAdd: (Int) -> Unit, onCustom: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onCustom,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add water", style = MaterialTheme.typography.titleMedium)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            presets.forEach { amount ->
                FilledTonalButton(
                    onClick = { onQuickAdd(amount) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Quick add ${Fmt.ml(amount)}" },
                ) {
                    Text("+${Fmt.number(amount)} mL")
                }
            }
        }
    }
}

@Composable
private fun PeriodSummaryRow(
    summaries: List<PeriodSummary>,
    selected: DayPeriod?,
    onSelect: (DayPeriod) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val horizontal = maxWidth / fontScale >= 330.dp
        if (horizontal) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                summaries.forEach { s ->
                    PeriodCard(s, s.period == selected, { onSelect(s.period) }, Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summaries.forEach { s ->
                    PeriodCard(s, s.period == selected, { onSelect(s.period) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PeriodCard(summary: PeriodSummary, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val share = summary.sharePercent
    val detail = when {
        share == null -> "No entries"
        summary.entryCount == 0 -> "No entries"
        else -> "${Fmt.entries(summary.entryCount)} · ${Fmt.percent(share)}"
    }
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                this.selected = selected
                contentDescription = "${summary.period.label}, ${summary.period.rangeLabel}: " +
                    "${Fmt.ml(summary.volumeMl)}, $detail" + if (selected) ". Filter active" else ""
            }
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = zoneColor(summary.period),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(summary.period.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(summary.period.rangeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(6.dp))
            Text(Fmt.ml(summary.volumeMl), style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EntryRow(entry: WaterEntry, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClickLabel = "Edit entry", onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Fmt.time(entry.localTime), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(64.dp))
            Column(Modifier.weight(1f)) {
                Text(Fmt.ml(entry.amountMl), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(entry.period.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun waveDescription(date: LocalDate, isToday: Boolean, wave: WaveModel, periods: List<PeriodSummary>): String {
    val sb = StringBuilder()
    sb.append("Water timeline for ").append(if (isToday) "today, " else "").append(Fmt.dateLong(date)).append(". ")
    if (wave.goalMl != null) {
        sb.append("${Fmt.ml(wave.totalMl)} recorded of a ${Fmt.ml(wave.goalMl)} goal, ${wave.percent} percent. ")
        if (wave.overflowMl > 0) sb.append("${Fmt.ml(wave.overflowMl)} above the goal. ")
    } else {
        sb.append("${Fmt.ml(wave.totalMl)} recorded. No goal set for this date. ")
    }
    if (wave.steps.isEmpty()) {
        sb.append("No entries.")
    } else {
        periods.forEach { p -> sb.append("${p.period.label}: ${Fmt.ml(p.volumeMl)}. ") }
        sb.append("Rises at ")
        sb.append(wave.steps.joinToString { "${Fmt.time(it.time)} plus ${Fmt.ml(it.addedMl)}" })
        sb.append(".")
    }
    return sb.toString()
}
