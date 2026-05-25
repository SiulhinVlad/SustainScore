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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sustainscore.domain.DailySummaryGenerator
import com.example.sustainscore.domain.HourlyScoreCalculator
import com.example.sustainscore.domain.ScoreCalculator
import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType
import com.example.sustainscore.model.DailySummary
import com.example.sustainscore.model.PredefinedActivities
import com.example.sustainscore.ui.logactivity.LogActivityScreen
import com.example.sustainscore.ui.logactivity.parseTimeOfDay
import com.example.sustainscore.ui.theme.AlertRed
import com.example.sustainscore.ui.theme.ForestGreen
import com.example.sustainscore.ui.theme.RiverBlue
import com.example.sustainscore.ui.theme.SunlitClay
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

internal enum class CurrentScoreRange(
    val minimumScore: Double,
    val displayColor: Color
) {
    Good(minimumScore = 80.0, displayColor = ForestGreen),
    Ok(minimumScore = 60.0, displayColor = RiverBlue),
    Bad(minimumScore = 40.0, displayColor = SunlitClay),
    Danger(minimumScore = 0.0, displayColor = AlertRed)
}

private enum class DashboardDestination {
    Dashboard,
    LogActivity
}

internal val DashboardDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

@Composable
fun DashboardRoute(modifier: Modifier = Modifier) {
    val activityTypes = remember { PredefinedActivities.all }
    val anchorDate = remember { LocalDate.now() }
    var allLogs by remember(anchorDate) { mutableStateOf(sampleLogsFor(anchorDate)) }
    var selectedDateEpochDay by rememberSaveable {
        mutableLongStateOf(anchorDate.toEpochDay())
    }
    var currentDestination by rememberSaveable {
        mutableStateOf(DashboardDestination.Dashboard)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val selectedDate = remember(selectedDateEpochDay) {
        LocalDate.ofEpochDay(selectedDateEpochDay)
    }
    val selectedDateLogs = remember(selectedDate, allLogs) {
        logsForDate(allLogs, selectedDate)
    }
    val uiState = remember(selectedDate, selectedDateLogs, activityTypes) {
        buildDashboardUiState(
            date = selectedDate,
            logs = selectedDateLogs,
            activityTypes = activityTypes
        )
    }

    when (currentDestination) {
        DashboardDestination.Dashboard -> DashboardScreen(
            state = uiState,
            selectedDate = selectedDate,
            anchorDate = anchorDate,
            allLogs = allLogs,
            activityTypes = activityTypes,
            snackbarHostState = snackbarHostState,
            onLogActivityClick = { currentDestination = DashboardDestination.LogActivity },
            onSelectedDateChange = { date ->
                selectedDateEpochDay = date.toEpochDay()
            },
            modifier = modifier
        )

        DashboardDestination.LogActivity -> LogActivityScreen(
            activityTypes = activityTypes,
            onBackClick = { currentDestination = DashboardDestination.Dashboard },
            onSave = { activityType, quantity, durationMinutes, startTime ->
                allLogs = allLogs + ActivityLog(
                    id = "log-${System.currentTimeMillis()}",
                    activityTypeId = activityType.id,
                    startTimestampMillis = timestampForTime(selectedDate, startTime),
                    quantity = quantity,
                    durationMinutes = durationMinutes
                )
                currentDestination = DashboardDestination.Dashboard
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("${activityType.name} added to today")
                }
            },
            modifier = modifier
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
    val currentScore = scoreCalculator.calculateActualScore(logs)
    val rawRecommendedScore = scoreCalculator.calculateRecommendedScore(logs)
    val recommendedScore = maxOf(rawRecommendedScore, currentScore)

    return DashboardUiState(
        displayDate = date.format(DashboardDateFormatter),
        currentScore = currentScore,
        recommendedScore = recommendedScore,
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
    selectedDate: LocalDate,
    anchorDate: LocalDate,
    allLogs: List<ActivityLog>,
    activityTypes: List<ActivityType>,
    snackbarHostState: SnackbarHostState,
    onLogActivityClick: () -> Unit,
    onSelectedDateChange: (LocalDate) -> Unit,
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
                .padding(innerPadding)
                .testTag("dashboard_list"),
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
                DailyUsageChartCard(
                    anchorDate = anchorDate,
                    selectedDate = selectedDate,
                    logs = allLogs,
                    activityTypes = activityTypes,
                    onSelectedDateChange = onSelectedDateChange
                )
            }
            item {
                DailySummaryCard(state = state)
            }
            item {
                Button(
                    onClick = onLogActivityClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_log_activity_button"),
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
                text = "Track this day's home habits and compare them with a more sustainable target.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (state.hasLogs) {
                    "${state.logCount} logged activities across ${state.trackedActivityCount} tracked categories."
                } else {
                    "No activities logged yet. Add your first habit to start this day's summary."
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
                    scoreColor = currentScoreRange(state.currentScore).displayColor,
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
                    scoreColor = currentScoreRange(state.currentScore).displayColor,
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
    scoreColor: Color = MaterialTheme.colorScheme.primary,
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
                color = scoreColor
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
                    text = "Once you log activities, this space will show your best hour and biggest sustainability impact.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
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
            startTimestampMillis = timestampForHour(date, 7),
            quantity = 3.0,
            durationMinutes = 60
        ),
        ActivityLog(
            id = "sample-2",
            activityTypeId = "washing_machine",
            startTimestampMillis = timestampForHour(date, 13),
            quantity = 1.0,
            durationMinutes = 60
        ),
        ActivityLog(
            id = "sample-3",
            activityTypeId = "computer_tv",
            startTimestampMillis = timestampForHour(date, 18),
            quantity = 2.0,
            durationMinutes = 60
        )
    )
}

private fun timestampForHour(date: LocalDate, hour: Int): Long {
    return LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun timestampForTime(date: LocalDate, timeText: String): Long {
    val (hour, minute) = requireNotNull(parseTimeOfDay(timeText)) {
        "Invalid time input: $timeText"
    }
    return LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

internal fun currentScoreRange(score: Double): CurrentScoreRange {
    return CurrentScoreRange.entries.first { score >= it.minimumScore }
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
            selectedDate = LocalDate.of(2026, 5, 18),
            anchorDate = LocalDate.of(2026, 5, 18),
            allLogs = sampleLogsFor(LocalDate.of(2026, 5, 18)),
            activityTypes = PredefinedActivities.all,
            snackbarHostState = SnackbarHostState(),
            onLogActivityClick = {},
            onSelectedDateChange = {}
        )
    }
}
