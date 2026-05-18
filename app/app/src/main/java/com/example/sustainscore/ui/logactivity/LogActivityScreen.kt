package com.example.sustainscore.ui.logactivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sustainscore.model.ActivityType
import com.example.sustainscore.model.PredefinedActivities
import com.example.sustainscore.ui.theme.SustainScoreTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogActivityScreen(
    activityTypes: List<ActivityType>,
    onBackClick: () -> Unit,
    onSave: (ActivityType, Double, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedActivityId by rememberSaveable { mutableStateOf(activityTypes.first().id) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("") }
    var startTime by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val selectedActivity = activityTypes.first { it.id == selectedActivityId }
    val parsedQuantity = quantity.toDoubleOrNull()
    val parsedDurationMinutes = parseDurationMinutes(duration)
    val quantityIsValid = parsedQuantity != null && parsedQuantity > 0.0
    val durationIsValid = parsedDurationMinutes != null && parsedDurationMinutes > 0
    val startTimeIsValid = isValidTimeOfDay(startTime)
    val formIsValid = quantityIsValid && durationIsValid && startTimeIsValid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
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
                            text = "Log Activity",
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
                .testTag("log_activity_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Add Usage",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = selectedActivity.name)
                                }
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
                            value = quantity,
                            onValueChange = { quantity = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_activity_quantity_field"),
                            label = { Text("Quantity") },
                            placeholder = { Text("Example: 2") },
                            supportingText = {
                                Text(
                                    text = "Recommended per hour: ${formatDecimal(selectedActivity.recommendedLimit)}"
                                )
                            },
                            isError = quantity.isNotBlank() && !quantityIsValid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )

                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_activity_duration_field"),
                            label = { Text("Duration") },
                            placeholder = { Text("H or HH:mm, e.g. 2 or 02:30") },
                            isError = duration.isNotBlank() && !durationIsValid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text
                            )
                        )

                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("log_activity_start_time_field"),
                            label = { Text("Start time") },
                            placeholder = { Text("H or HH:mm, e.g. 7 or 18:00") },
                            isError = startTime.isNotBlank() && !startTimeIsValid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("log_activity_back_button")
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    onSave(
                                        selectedActivity,
                                        parsedQuantity ?: 0.0,
                                        parsedDurationMinutes ?: 0,
                                        startTime
                                    )
                                },
                                enabled = formIsValid,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("log_activity_save_button")
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

internal fun parseDurationMinutes(durationText: String): Int? {
    val normalized = durationText.trim()
    val singleHour = normalized.toIntOrNull()

    if (singleHour != null) {
        return if (singleHour in 0..23) {
            singleHour * 60
        } else {
            null
        }
    }

    val parts = normalized.split(":")
    if (parts.size != 2) {
        return null
    }

    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null

    if (hours < 0 || minutes !in 0..59) {
        return null
    }

    return hours * 60 + minutes
}

internal fun isValidTimeOfDay(timeText: String): Boolean {
    return parseTimeOfDay(timeText) != null
}

internal fun parseTimeOfDay(timeText: String): Pair<Int, Int>? {
    val normalized = timeText.trim()
    val singleHour = normalized.toIntOrNull()

    if (singleHour != null) {
        return if (singleHour in 0..23) {
            singleHour to 0
        } else {
            null
        }
    }

    val parts = normalized.split(":")
    if (parts.size != 2) {
        return null
    }

    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null

    return if (hour in 0..23 && minute in 0..59) {
        hour to minute
    } else {
        null
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun LogActivityScreenPreview() {
    SustainScoreTheme {
        LogActivityScreen(
            activityTypes = PredefinedActivities.all,
            onBackClick = {},
            onSave = { _, _, _, _ -> }
        )
    }
}
