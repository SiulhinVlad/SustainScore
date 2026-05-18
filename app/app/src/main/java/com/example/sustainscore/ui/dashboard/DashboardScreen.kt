package com.example.sustainscore.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sustainscore.domain.DailySummaryGenerator
import com.example.sustainscore.domain.HourlyScoreCalculator
import com.example.sustainscore.domain.ScoreCalculator
import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType
import com.example.sustainscore.model.ActivityUnit
import com.example.sustainscore.model.DailySummary
import com.example.sustainscore.model.PredefinedActivities
import com.example.sustainscore.ui.theme.SustainScoreTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class DashboardUiState(
    val displayDate: String,
    val currentScore: Double,
    val recommendedScore: Double,
    val dailySummary: DailySummary,
    val logCount: Int,
    val trackedActivityCount: Int
) {
    val hasLogs: Boolean = logCount > 0
}

@Composable
fun DashboardRoute(modifier: Modifier = Modifier) {
    val activityTypes = remember { PredefinedActivities.all }
    val today = remember { LocalDate.now() }
    var logs by remember(today) { mutableStateOf(sampleLogsFor(today)) }
    var showLogActivityDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val uiState = remember(today, logs, activityTypes) {
        buildDashboardUiState(
            date = today,
            logs = logs,
            activityTypes = activityTypes
        )
    }

    DashboardScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onLogActivityClick = { showLogActivityDialog = true },
        modifier = modifier
    )

    if (showLogActivityDialog) {
        LogActivityDialog(
            activityTypes = activityTypes,
            onDismiss = { showLogActivityDialog = false },
            onSave = { activityType, amount ->
                logs = logs + ActivityLog(
                    id = "log-${System.currentTimeMillis()}",
                    activityTypeId = activityType.id,
                    timestampMillis = System.currentTimeMillis(),
                    amount = amount
                )
                showLogActivityDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("${activityType.name} added to today")
                }
            }
        )
    }
}

internal fun buildDashboardUiState(
    date: LocalDate,
    logs: List<ActivityLog>,
    activityTypes: List<ActivityType> = PredefinedActivities.all
): DashboardUiState {
    val scoreCalculator = ScoreCalculator(activityTypes)
    val dailySummaryGenerator = DailySummaryGenerator(
        activityTypes = activityTypes,
        hourlyScoreCalculator = HourlyScoreCalculator(scoreCalculator)
    )

    return DashboardUiState(
        displayDate = date.format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
        ),
        currentScore = scoreCalculator.calculateActualScore(logs),
        recommendedScore = scoreCalculator.calculateRecommendedScore(logs),
        dailySummary = dailySummaryGenerator.generate(
            date = date.format(DateTimeFormatter.ISO_DATE),
            logs = logs
        ),
        logCount = logs.size,
        trackedActivityCount = logs.map { it.activityTypeId }.distinct().size
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onLogActivityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SustainScore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OverviewCard(state = state)
            }
            item {
                ScoreSection(state = state)
            }
            item {
                DailySummaryCard(state = state)
            }
            item {
                Button(
                    onClick = onLogActivityClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(text = "Log Activity")
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(state: DashboardUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = state.displayDate,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Track today's home habits and compare them with a more sustainable target.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (state.hasLogs) {
                    "${state.logCount} logged activities across ${state.trackedActivityCount} tracked categories."
                } else {
                    "No activities logged yet. Add your first habit to start today's summary."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScoreSection(state: DashboardUiState) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compactLayout = maxWidth < 420.dp

        if (compactLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScoreCard(
                    title = "Current score",
                    score = state.currentScore,
                    supportingText = if (state.hasLogs) {
                        "Live score based on today's logged activities."
                    } else {
                        "Start logging to calculate your daily score."
                    }
                )
                ScoreCard(
                    title = "Recommended score",
                    score = state.recommendedScore,
                    supportingText = if (state.hasLogs) {
                        "Target score for the same activity types."
                    } else {
                        "Your sustainable target will appear here."
                    }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScoreCard(
                    title = "Current score",
                    score = state.currentScore,
                    supportingText = if (state.hasLogs) {
                        "Live score based on today's logged activities."
                    } else {
                        "Start logging to calculate your daily score."
                    },
                    modifier = Modifier.weight(1f)
                )
                ScoreCard(
                    title = "Recommended score",
                    score = state.recommendedScore,
                    supportingText = if (state.hasLogs) {
                        "Target score for the same activity types."
                    } else {
                        "Your sustainable target will appear here."
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(
    title: String,
    score: Double,
    supportingText: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${score.roundToInt()} / 100",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailySummaryCard(state: DashboardUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Daily summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (!state.hasLogs) {
                Text(
                    text = "Once you log activities, this space will show your average score, best hour, and biggest sustainability impact.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SummaryMetric(
                    label = "Average score",
                    value = "${formatDecimal(state.dailySummary.averageScore)} / 100"
                )
                SummaryMetric(
                    label = "Best hour",
                    value = state.dailySummary.bestHour?.toHourLabel() ?: "Not available yet"
                )
                SummaryMetric(
                    label = "Highest impact activity",
                    value = state.dailySummary.highestImpactActivityName ?: "No dominant activity yet"
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = buildRecommendationText(state),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LogActivityDialog(
    activityTypes: List<ActivityType>,
    onDismiss: () -> Unit,
    onSave: (ActivityType, Double) -> Unit
) {
    var selectedActivityId by rememberSaveable { mutableStateOf(activityTypes.first().id) }
    var amount by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val selectedActivity = activityTypes.first { it.id == selectedActivityId }
    val parsedAmount = amount.toDoubleOrNull()
    val amountIsValid = parsedAmount != null && parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Log Activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Add a domestic activity and the dashboard will update immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Activity type",
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedActivity.name,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = selectedActivity.unit.label(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        activityTypes.forEach { activity ->
                            DropdownMenuItem(
                                text = { Text(activity.name) },
                                onClick = {
                                    selectedActivityId = activity.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount") },
                    placeholder = { Text("Enter ${selectedActivity.unit.label()}") },
                    supportingText = {
                        Text(
                            text = "Recommended daily limit: ${
                                formatDecimal(selectedActivity.recommendedLimit)
                            } ${selectedActivity.unit.label()}."
                        )
                    },
                    isError = amount.isNotBlank() && !amountIsValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedActivity, parsedAmount ?: 0.0) },
                enabled = amountIsValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

internal fun buildRecommendationText(state: DashboardUiState): String {
    val improvementActivity = state.dailySummary.highestImpactActivityName
    return when {
        !state.hasLogs -> "Add your first activity to receive a sustainability suggestion."
        state.currentScore >= state.recommendedScore ->
            "You are already meeting or beating the recommended score for today's activity mix. Keep the same pattern going."
        improvementActivity != null ->
            "Focusing on $improvementActivity is the clearest way to move closer to today's recommended score."
        else ->
            "Reducing your highest-use activity today would move you closer to the recommended score."
    }
}

private fun sampleLogsFor(date: LocalDate): List<ActivityLog> {
    return listOf(
        ActivityLog(
            id = "sample-1",
            activityTypeId = "lighting",
            timestampMillis = timestampForHour(date, 7),
            amount = 3.0
        ),
        ActivityLog(
            id = "sample-2",
            activityTypeId = "washing_machine",
            timestampMillis = timestampForHour(date, 13),
            amount = 1.0
        ),
        ActivityLog(
            id = "sample-3",
            activityTypeId = "computer_tv",
            timestampMillis = timestampForHour(date, 18),
            amount = 2.0
        )
    )
}

private fun timestampForHour(date: LocalDate, hour: Int): Long {
    return LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun ActivityUnit.label(): String {
    return when (this) {
        ActivityUnit.COUNT -> "count"
        ActivityUnit.MINUTES -> "minutes"
        ActivityUnit.HOURS -> "hours"
    }
}

private fun Int.toHourLabel(): String {
    return String.format(Locale.getDefault(), "%02d:00", this)
}

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun DashboardScreenPreview() {
    SustainScoreTheme {
        DashboardScreen(
            state = buildDashboardUiState(
                date = LocalDate.of(2026, 5, 18),
                logs = sampleLogsFor(LocalDate.of(2026, 5, 18))
            ),
            snackbarHostState = SnackbarHostState(),
            onLogActivityClick = {}
        )
    }
}
