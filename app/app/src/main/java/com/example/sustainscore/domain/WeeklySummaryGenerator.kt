package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType
import com.example.sustainscore.model.WeeklySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeeklySummaryGenerator(
    activityTypes: List<ActivityType>,
    private val scoreCalculator: ScoreCalculator
) {
    private val activityTypeById = activityTypes.associateBy { it.id }

    fun generate(
        endDate: LocalDate,
        logs: List<ActivityLog>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeeklySummary {
        val startDate = endDate.minusDays(6)
        val weekLogs = logs.filter { log ->
            val logDate = Instant.ofEpochMilli(log.startTimestampMillis)
                .atZone(zoneId)
                .toLocalDate()
            !logDate.isBefore(startDate) && !logDate.isAfter(endDate)
        }

        val averageScore = (0L..6L)
            .map { dayOffset ->
                val currentDate = startDate.plusDays(dayOffset)
                val dailyLogs = weekLogs.filter { log ->
                    Instant.ofEpochMilli(log.startTimestampMillis)
                        .atZone(zoneId)
                        .toLocalDate() == currentDate
                }
                scoreCalculator.calculateActualScore(dailyLogs)
            }
            .average()

        val highestImpactActivityName = weekLogs
            .mapNotNull { log ->
                val activityType = activityTypeById[log.activityTypeId] ?: return@mapNotNull null
                activityType.name to log.quantityHours * activityType.weight
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, impacts) -> impacts.sum() }
            .maxByOrNull { it.value }
            ?.key

        return WeeklySummary(
            startDate = startDate,
            endDate = endDate,
            highestImpactActivityName = highestImpactActivityName,
            averageScore = averageScore
        )
    }
}
