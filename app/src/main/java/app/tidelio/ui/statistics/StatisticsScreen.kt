@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.tidelio.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tidelio.R
import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.statistics.StatisticsResult
import app.tidelio.ui.common.ContentWidth
import app.tidelio.ui.common.Fmt
import app.tidelio.ui.theme.TideColors
import app.tidelio.ui.wave.AxisLabels
import app.tidelio.ui.wave.zoneColor
import kotlin.math.max

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showInfo by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        ContentWidth(Modifier.padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30).forEach { days ->
                        FilterChip(
                            selected = state.rangeDays == days,
                            onClick = { viewModel.setRange(days) },
                            label = { Text("$days days") },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                val result = state.result
                if (result != null) {
                    Text(
                        "${Fmt.dateShort(result.startDate)} – ${Fmt.dateMediumYear(result.endDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                InfoPanel(expanded = showInfo, onToggle = { showInfo = !showInfo })
                when {
                    !state.loaded || result == null -> Unit
                    !result.hasAnyRecords -> Text(
                        "Add entries to see your recorded patterns.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> {
                        Metrics(result)
                        SectionTitle("Recorded volume by date")
                        DailyBars(result)
                        SectionTitle("Morning / Day / Evening")
                        Distribution(result)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Metrics(result: StatisticsResult) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val cardModifier = Modifier.weight(1f).widthIn(min = 140.dp)
        MetricCard("Total recorded", Fmt.ml(result.totalMl), cardModifier)
        MetricCard(
            "Days with records",
            "${result.daysWithRecords} of ${result.days.size}",
            cardModifier,
        )
        MetricCard(
            "Average on days with entries",
            result.averageOnDaysWithEntriesMl?.let { Fmt.ml(it) } ?: "—",
            cardModifier,
        )
        MetricCard(
            "Goal reached",
            if (result.recordedDaysWithGoal == 0) {
                "No recorded days with a goal"
            } else {
                "${result.goalMetDays} of ${result.recordedDaysWithGoal} recorded days with a goal"
            },
            cardModifier,
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Bars for days with records. Days without records show a hollow ring on the baseline rather
 * than a zero-height bar, because missing records are not evidence of zero intake.
 */
@Composable
private fun DailyBars(result: StatisticsResult) {
    val maxValue = max(
        result.days.maxOf { it.totalMl ?: 0L },
        result.days.maxOf { (it.goalMl ?: 0).toLong() },
    ).coerceAtLeast(1L)
    val description = buildString {
        append("Recorded volume by date. ")
        result.days.forEach { d ->
            append(Fmt.dateShort(d.date)).append(": ")
            append(d.totalMl?.let { Fmt.ml(it) } ?: "no records")
            if (d.goalMet) append(", goal reached")
            append(". ")
        }
    }
    Column(Modifier.clearAndSetSemantics { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            val n = result.days.size
            val slot = size.width / n
            val barW = slot * if (n <= 7) 0.5f else 0.62f
            val top = 10.dp.toPx()
            val bottom = size.height - 8.dp.toPx()
            val plotH = bottom - top
            drawLine(TideColors.Outline, Offset(0f, bottom), Offset(size.width, bottom), strokeWidth = 1.dp.toPx())
            result.days.forEachIndexed { i, d ->
                val cx = slot * i + slot / 2
                val total = d.totalMl
                if (total != null) {
                    val h = plotH * (total.toFloat() / maxValue)
                    drawRoundRect(
                        color = if (d.goalMet) TideColors.Teal else TideColors.WaveEdge.copy(alpha = 0.7f),
                        topLeft = Offset(cx - barW / 2, bottom - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                } else {
                    drawCircle(
                        color = TideColors.SlateMuted,
                        radius = (barW / 2).coerceAtMost(4.dp.toPx()),
                        center = Offset(cx, bottom - 6.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                d.goalMl?.let { goal ->
                    val gy = bottom - plotH * (goal.toFloat() / maxValue)
                    drawLine(
                        color = TideColors.Slate,
                        start = Offset(cx - slot * 0.42f, gy),
                        end = Offset(cx + slot * 0.42f, gy),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
                    )
                }
            }
        }
        val n = result.days.size
        val labels = result.days.mapIndexedNotNull { i, d ->
            val show = if (n <= 7) {
                true
            } else {
                i == n - 1 || (i % 7 == 0 && n - 1 - i >= 4)
            }
            if (show) ((i + 0.5f) / n) to (if (n <= 7) Fmt.weekdayShort(d.date) else Fmt.dateShort(d.date)) else null
        }
        AxisLabels(labels, Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(TideColors.Teal, RoundedCornerShape(2.dp)))
            Text("  Goal reached   ", style = MaterialTheme.typography.bodySmall)
            Box(Modifier.size(10.dp).background(TideColors.WaveEdge.copy(alpha = 0.7f), RoundedCornerShape(2.dp)))
            Text("  Recorded", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("○ No records (not zero intake)   ┄ Goal for that date", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Distribution(result: StatisticsResult) {
    val description = DayPeriod.entries.joinToString(". ") { p ->
        "${p.label}: ${Fmt.ml(result.periodTotals[p] ?: 0L)}, ${result.periodShare(p)?.let { Fmt.percent(it) } ?: "no entries"}"
    }
    Column(Modifier.clearAndSetSemantics { contentDescription = "Distribution by period. $description" }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp)),
        ) {
            var x = 0f
            DayPeriod.entries.forEach { p ->
                val share = (result.periodShare(p) ?: 0.0).toFloat() / 100f
                val w = size.width * share
                if (w > 0f) {
                    drawRect(zoneColor(p), topLeft = Offset(x, 0f), size = Size(w, size.height))
                    if (x > 0f) drawLine(TideColors.Surface, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                }
                x += w
            }
        }
        Spacer(Modifier.height(8.dp))
        DayPeriod.entries.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(zoneColor(p), RoundedCornerShape(3.dp)))
                Text(
                    "  ${p.label} (${p.rangeLabel})",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${Fmt.ml(result.periodTotals[p] ?: 0L)} · ${result.periodShare(p)?.let { Fmt.percent(it) } ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun InfoPanel(expanded: Boolean, onToggle: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            TextButton(onClick = onToggle, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(painterResource(R.drawable.ic_info), contentDescription = null)
                Text(
                    if (expanded) "  Hide how metrics are calculated" else "  How metrics are calculated",
                )
            }
            if (expanded) {
                val lines = listOf(
                    "Range: the last 7 or 30 dates, ending today.",
                    "Total recorded: sum of all entries in the range.",
                    "Days with records: dates with at least one entry.",
                    "Average on days with entries: total ÷ days with records. Days without records are " +
                        "excluded — missing records are not treated as zero intake.",
                    "Period distribution: period volume ÷ total volume × 100. " +
                        "Morning 00:00–11:59, Day 12:00–17:59, Evening 18:00–23:59 (time as recorded).",
                    "Goal reached: recorded days whose total is at least the goal saved for that date, " +
                        "out of recorded days that had a goal.",
                    "These are records of what you entered, not health scores or medical interpretations.",
                )
                lines.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp))
                }
            }
        }
    }
}
