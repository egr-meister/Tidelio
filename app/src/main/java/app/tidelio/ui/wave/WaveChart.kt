package app.tidelio.ui.wave

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.statistics.WaveModel
import app.tidelio.ui.common.Fmt
import app.tidelio.ui.theme.TideColors
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

fun zoneColor(period: DayPeriod): Color = when (period) {
    DayPeriod.MORNING -> TideColors.Morning
    DayPeriod.DAY -> TideColors.Day
    DayPeriod.EVENING -> TideColors.Evening
}

/**
 * The "Wave of the Day": a step-based cumulative timeline over 00:00–24:00.
 *
 * The data boundary (edge line and markers) is an exact step function of recorded entries.
 * The optional ripple after adding an entry is drawn INSIDE the filled area only and never
 * moves the boundary, so it cannot change the represented values.
 */
@Composable
fun WaveChart(
    model: WaveModel,
    nowFraction: Float?,
    selectedPeriod: DayPeriod?,
    rippleKey: Int,
    animationsEnabled: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(rippleKey, animationsEnabled) {
        if (rippleKey > 0 && animationsEnabled) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(durationMillis = 1600, easing = LinearEasing))
        } else {
            ripple.snapTo(1f)
        }
    }

    Column(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(Modifier.fillMaxWidth()) {
            DayPeriod.entries.forEach { period ->
                val hours = (period.endHour - period.startHour + 1).toFloat()
                Text(
                    text = period.label,
                    modifier = Modifier.weight(hours).padding(bottom = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedPeriod == null || period == selectedPeriod) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            WaveCanvas(model, nowFraction, selectedPeriod, ripple.value, Modifier.matchParentSize())
            val scaleLabel = if (model.goalMl != null) {
                "Goal ${Fmt.ml(model.goalMl)}"
            } else {
                "Scale 0–${Fmt.ml(model.scaleMaxMl)}"
            }
            Text(
                text = scaleLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(TideColors.Surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        AxisLabels(
            labels = listOf(0f to "00:00", 0.25f to "06:00", 0.5f to "12:00", 0.75f to "18:00", 1f to "24:00"),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun WaveCanvas(
    model: WaveModel,
    nowFraction: Float?,
    selectedPeriod: DayPeriod?,
    ripplePhase: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val top = h * 0.17f
        val bottom = h
        val plotH = bottom - top
        fun yFor(ml: Long): Float = bottom - plotH * model.heightFraction(ml)

        // Period zones.
        DayPeriod.entries.forEach { period ->
            val alpha = if (selectedPeriod == null || selectedPeriod == period) 1f else 0.35f
            drawRect(
                color = zoneColor(period).copy(alpha = alpha),
                topLeft = Offset(period.startFraction * w, 0f),
                size = Size((period.endFraction - period.startFraction) * w, h),
            )
        }
        // Faint hour guides at 06, 12, 18.
        listOf(0.25f, 0.5f, 0.75f).forEach { f ->
            drawLine(TideColors.Outline.copy(alpha = 0.5f), Offset(f * w, 0f), Offset(f * w, h), strokeWidth = 1.dp.toPx())
        }

        // Later today: lighter background for hours that have not happened yet.
        val nowX = nowFraction?.let { it.coerceIn(0f, 1f) * w }
        if (nowX != null && nowX < w) {
            drawRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(nowX, 0f), size = Size(w - nowX, h))
        }

        // Step path of cumulative volume.
        val lastStepX = model.steps.lastOrNull()?.x?.times(w) ?: 0f
        val endX = max(nowX ?: w, lastStepX)
        val edge = Path()
        val fill = Path()
        var y = bottom
        edge.moveTo(0f, y)
        fill.moveTo(0f, bottom)
        fill.lineTo(0f, y)
        model.steps.forEach { step ->
            val x = step.x * w
            edge.lineTo(x, y)
            fill.lineTo(x, y)
            y = yFor(step.cumulativeMl)
            edge.lineTo(x, y)
            fill.lineTo(x, y)
        }
        edge.lineTo(endX, y)
        fill.lineTo(endX, y)
        fill.lineTo(endX, bottom)
        fill.close()

        drawPath(fill, TideColors.WaveFill)

        // Decorative ripple: clipped to the filled area, fades out, never touches the boundary.
        if (ripplePhase > 0f && ripplePhase < 1f && model.totalMl > 0) {
            clipPath(fill) {
                val amplitude = 4.dp.toPx()
                val waveLength = w / 5f
                val baseY = bottom - (bottom - y) * 0.45f
                val shift = ripplePhase * waveLength * 2f
                val band = Path()
                band.moveTo(0f, bottom)
                var x = 0f
                while (x <= w) {
                    val yy = baseY + amplitude * sin(((x + shift) / waveLength) * 2f * PI.toFloat())
                    band.lineTo(x, yy)
                    x += 6f
                }
                band.lineTo(w, bottom)
                band.close()
                drawPath(band, Color.White.copy(alpha = 0.35f * (1f - ripplePhase)))
            }
        }

        drawPath(edge, TideColors.WaveEdge, style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))

        if (nowX != null && nowX < w) {
            drawLine(
                color = TideColors.SlateMuted,
                start = Offset(nowX, 0f),
                end = Offset(nowX, h),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
        }

        // Goal line: the wave's goal-relative drawing is capped here.
        if (model.goalMl != null) {
            drawLine(
                color = TideColors.Teal,
                start = Offset(0f, top),
                end = Offset(w, top),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())),
            )
        }

        // Entry markers at their actual times.
        model.steps.forEach { step ->
            val c = Offset(step.x * w, yFor(step.cumulativeMl))
            val dimmed = selectedPeriod != null && step.period != selectedPeriod
            drawCircle(Color.White, radius = 5.5.dp.toPx(), center = c)
            drawCircle(
                if (dimmed) TideColors.Teal.copy(alpha = 0.35f) else TideColors.TealDark,
                radius = 3.5.dp.toPx(),
                center = c,
            )
        }

        // Selected zone outline (shape cue in addition to color).
        selectedPeriod?.let { period ->
            drawRect(
                color = TideColors.Teal,
                topLeft = Offset(period.startFraction * w, 1.dp.toPx()),
                size = Size((period.endFraction - period.startFraction) * w, h - 2.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
fun AxisLabels(labels: List<Pair<Float, String>>, modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            labels.forEach { (_, text) ->
                Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { i, p ->
                val center = (labels[i].first * width).toInt()
                val x = (center - p.width / 2).coerceIn(0, max(0, width - p.width))
                p.placeRelative(x, 0)
            }
        }
    }
}
