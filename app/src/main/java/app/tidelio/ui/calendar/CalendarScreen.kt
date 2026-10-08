@file:OptIn(ExperimentalMaterial3Api::class)

package app.tidelio.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tidelio.R
import app.tidelio.ui.common.ContentWidth
import app.tidelio.ui.common.Fmt
import app.tidelio.ui.theme.TideColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onOpenDay: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        ContentWidth(Modifier.padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { viewModel.showMonth(state.month.minusMonths(1)) }) {
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous month")
                    }
                    Text(
                        Fmt.month(state.month),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    IconButton(onClick = { viewModel.showMonth(state.month.plusMonths(1)) }) {
                        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next month")
                    }
                }
                if (state.month != java.time.YearMonth.from(state.today)) {
                    TextButton(
                        onClick = { viewModel.showMonth(java.time.YearMonth.from(state.today)) },
                        modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                    ) { Text("Go to current month") }
                }
                Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                    (0 until 7).forEach { i ->
                        val dow = state.firstDayOfWeek.plus(i.toLong())
                        Text(
                            dow.getDisplayName(TextStyle.SHORT, Locale.US),
                            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            Box(Modifier.weight(1f).padding(2.dp)) {
                                if (cell != null) {
                                    DayCell(
                                        cell = cell,
                                        maxTotal = state.maxMonthTotal,
                                        onClick = {
                                            viewModel.select(cell.date)
                                            if (cell.status != DayStatus.FUTURE) onOpenDay(cell.date)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val selected = state.selected
                if (selected != null && selected.isAfter(state.today)) {
                    Text(Fmt.dateLong(selected), style = MaterialTheme.typography.titleMedium)
                    Text("No records yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                }
                Legend()
            }
        }
    }
}

@Composable
private fun DayCell(cell: CalendarCell, maxTotal: Long, onClick: () -> Unit) {
    val description = buildString {
        append(Fmt.dateLong(cell.date))
        if (cell.isToday) append(", today")
        append(". ")
        when (cell.status) {
            DayStatus.FUTURE -> append("Future date, no records yet.")
            DayStatus.NO_RECORD -> append("No records.")
            DayStatus.RECORDED -> {
                append("${Fmt.ml(cell.totalMl ?: 0L)} recorded. ")
                when {
                    cell.goalMl == null -> append("No goal set for this date.")
                    cell.goalMet -> append("Goal of ${Fmt.ml(cell.goalMl)} reached.")
                    else -> append("Goal of ${Fmt.ml(cell.goalMl)} not reached.")
                }
            }
        }
    }
    val shape = RoundedCornerShape(12.dp)
    var modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(0.9f)
        .heightIn(min = 48.dp)
        .clip(shape)
    modifier = if (cell.isSelected) modifier.background(TideColors.TealContainer) else modifier
    modifier = if (cell.isToday) modifier.border(2.dp, TideColors.Teal, shape) else modifier
    Column(
        modifier
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .alpha(if (cell.status == DayStatus.FUTURE) 0.45f else 1f)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            cell.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
        )
        Box(Modifier.height(18.dp), contentAlignment = Alignment.Center) {
            when {
                cell.status == DayStatus.RECORDED && cell.goalMet -> Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = TideColors.TealDark,
                    modifier = Modifier.size(16.dp),
                )
                cell.status == DayStatus.RECORDED -> {
                    val fraction = if (maxTotal > 0) (cell.totalMl ?: 0L).toFloat() / maxTotal else 1f
                    val dot = (6 + 6 * fraction).dp
                    Box(Modifier.size(dot).background(TideColors.WaveEdge, CircleShape))
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun Legend() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 16.dp)) {
        Text("Legend", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(TideColors.WaveEdge, CircleShape))
            Text("  Water recorded (larger dot = more volume)", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = TideColors.TealDark, modifier = Modifier.size(14.dp))
            Text("  Goal reached (days with a goal only)", style = MaterialTheme.typography.bodySmall)
        }
        Text("Number only: no records for that day (not the same as zero intake)", style = MaterialTheme.typography.bodySmall)
        Text("Faded: future date", style = MaterialTheme.typography.bodySmall)
        Text("Outlined: today", style = MaterialTheme.typography.bodySmall)
    }
}
