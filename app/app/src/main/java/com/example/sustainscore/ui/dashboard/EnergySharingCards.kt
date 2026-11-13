package com.example.sustainscore.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sustainscore.domain.EnergyPrediction
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

@Composable
internal fun EnergyPredictionCard(prediction: EnergyPrediction?, onCalculate: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("energy_prediction_card"),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Predictive energy sharing", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            if (prediction == null) {
                Text("See how your next week fits with your neighbours’ demand, then find a better time to use shared energy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = onCalculate,
                    modifier = Modifier.fillMaxWidth().testTag("calculate_prediction_button"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 25.dp)
                ) {
                    Text("Calculate prediction with AI", textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium)
                }
                DemoCaption("Demo: local estimates from your logs and four sample neighbours. No live AI or meter connection.")
            } else {
                var selectedDay by rememberSaveable { mutableIntStateOf(0) }
                val selected = prediction.days[selectedDay.coerceIn(prediction.days.indices)]
                val dateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
                Text("${prediction.days.first().date.format(dateFormat)} – ${prediction.days.last().date.format(dateFormat)} · next 7 days",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                DemoCaption("Based on ${prediction.historyLogCount} recent logs across ${prediction.historyDayCount} days. Appliance estimates + assumed background use. Both charts show daily kWh on the same dates; vertical scales differ.")

                ForecastLineChart("Your predicted usage", prediction.days.map { it.userKwh },
                    prediction, MaterialTheme.colorScheme.primary, selectedDay,
                    { selectedDay = it }, "user_prediction_chart")
                HorizontalDivider()
                ForecastLineChart("Other users · 4 homes combined", prediction.days.map { it.neighboursKwh },
                    prediction, MaterialTheme.colorScheme.tertiary, selectedDay,
                    { selectedDay = it }, "neighbours_prediction_chart")

                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(selected.date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)),
                            style = MaterialTheme.typography.labelLarge)
                        Text("You: ${kwh(selected.userKwh)} kWh · Neighbours: ${kwh(selected.neighboursKwh)} kWh",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("selected_prediction_values"))
                        Text("Tap either chart to compare a day.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = onCalculate, modifier = Modifier.align(Alignment.End)
                    .testTag("update_prediction_button")) {
                    Text("Update prediction with AI")
                }
                DemoCaption("Demo forecast · neighbours and solar are simulated. Your forecast and suggestions refresh when you log or edit usage.")
            }
        }
    }
}

@Composable
private fun ForecastLineChart(
    title: String, values: List<Double>, prediction: EnergyPrediction, color: Color,
    selectedDay: Int, onSelectDay: (Int) -> Unit, tag: String
) {
    val top = ceil((values.maxOrNull() ?: 1.0).coerceAtLeast(1.0) * 1.15)
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val surface = MaterialTheme.colorScheme.surface
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text("${kwh(values.sum())} kWh this week", style = MaterialTheme.typography.bodySmall,
            color = color)
        Row(Modifier.fillMaxWidth().height(124.dp)) {
            Column(Modifier.width(38.dp).height(124.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("${top.toInt()}", style = MaterialTheme.typography.labelSmall)
                Text("kWh", style = MaterialTheme.typography.labelSmall)
                Text("0", style = MaterialTheme.typography.labelSmall)
            }
            Canvas(Modifier.weight(1f).height(124.dp).testTag(tag)
                .semantics {
                    contentDescription = title + ". " + prediction.days.mapIndexed { i, day ->
                        "${day.date}: ${kwh(values[i])} kWh"
                    }.joinToString("; ")
                }
                .pointerInput(values) {
                    detectTapGestures { point ->
                        onSelectDay((point.x / size.width * values.size).toInt().coerceIn(values.indices))
                    }
                }) {
                val inset = 7.dp.toPx()
                val chartHeight = size.height - 2 * inset
                val baseline = size.height - inset
                val points = values.mapIndexed { index, value ->
                    Offset(size.width * (index + 0.5f) / values.size,
                        baseline - (value / top).toFloat() * chartHeight)
                }
                (0..2).forEach { tick ->
                    val y = baseline - chartHeight * tick / 2
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val selectionX = points[selectedDay.coerceIn(points.indices)].x
                drawLine(color.copy(alpha = 0.22f), Offset(selectionX, inset),
                    Offset(selectionX, baseline), 1.dp.toPx())
                val line = Path().apply {
                    points.forEachIndexed { i, point ->
                        if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }
                drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                points.forEachIndexed { index, point ->
                    drawCircle(surface, if (index == selectedDay) 6.dp.toPx() else 4.dp.toPx(), point)
                    drawCircle(color, if (index == selectedDay) 5.dp.toPx() else 3.dp.toPx(), point)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 38.dp)) {
            prediction.days.forEach { day ->
                Text(day.date.format(DateTimeFormatter.ofPattern("EEE\ndd", Locale.ENGLISH)),
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 10.sp, lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun EnergySuggestionsCard(prediction: EnergyPrediction?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("ai_suggestions_card"),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("AI Generated Suggestions", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            if (prediction == null) {
                Text("Calculate your prediction above to see a plan based on your routine and the community’s demand.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                DemoCaption("Demo advice from local rules, your logs and the forecasts above. Choose changes that fit your household.")
                prediction.suggestions.forEachIndexed { index, suggestion ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${index + 1}. ${suggestion.title}", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(suggestion.explanation, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("energy_suggestion_$index"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoCaption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun kwh(value: Double) = String.format(Locale.ENGLISH, "%.1f", value)
