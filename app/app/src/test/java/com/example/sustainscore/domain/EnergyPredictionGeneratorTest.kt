package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EnergyPredictionGeneratorTest {
    private val zone = ZoneId.of("Europe/Luxembourg")
    private val today = LocalDate.of(2026, 11, 13)
    private val generator = EnergyPredictionGenerator(zone)

    private fun log(id: String, type: String, date: LocalDate, hour: Int, minutes: Int, quantity: Double = 1.0) =
        ActivityLog(id, type, date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(), quantity, minutes)

    @Test fun forecastsExactlyTheNextSevenDaysWithFiniteValuesAndAnHonestEmptyState() {
        val result = generator.generate(emptyList(), today)
        assertEquals((1L..7L).map(today::plusDays), result.days.map { it.date })
        assertTrue(result.days.all { it.userKwh.isFinite() && it.userKwh >= 0 })
        assertEquals(2.88, result.days.first().userKwh, 0.0001)
        assertEquals(0, result.historyLogCount)
        assertTrue(result.suggestions.first().explanation.contains("no recent activity"))
        assertTrue(result.days.first().neighboursKwh > result.days[2].neighboursKwh)
    }

    @Test fun addingUsageChangesUserForecastButKeepsFictionalNeighboursStable() {
        val before = generator.generate(listOf(log("light", "lighting", today, 7, 60)), today)
        val after = generator.generate(listOf(log("light", "lighting", today, 7, 60),
            log("heat", "heating_cooling", today, 19, 180)), today)
        assertTrue(after.days.sumOf { it.userKwh } > before.days.sumOf { it.userKwh })
        assertEquals(before.days.map { it.neighboursKwh }, after.days.map { it.neighboursKwh })
        assertEquals(2, after.historyLogCount)
        assertTrue(after.suggestions.any { it.explanation.contains("17:00") })
    }

    @Test fun movingAndDeletingLogsChangeForecastAndSuggestions() {
        val monday = LocalDate.of(2026, 11, 9)
        val lighting = log("light", "lighting", monday, 7, 60, 3.0)
        val laundry = log("wash", "washing_machine", today.minusDays(6), 11, 120)
        val original = generator.generate(listOf(lighting, laundry), today)
        assertTrue(original.suggestions.first().explanation.contains("weekend laundry"))
        assertTrue(original.suggestions.first().explanation.contains("Monday mornings"))
        val moved = generator.generate(listOf(lighting.copy(
            startTimestampMillis = monday.atTime(19, 0).atZone(zone).toInstant().toEpochMilli()), laundry), today)
        assertNotEquals(original.days[2].userHourlyKwh, moved.days[2].userHourlyKwh)
        assertFalse(moved.suggestions.first().explanation.contains("Monday mornings"))
        val deleted = generator.generate(listOf(lighting), today)
        assertTrue(deleted.days.sumOf { it.userKwh } < original.days.sumOf { it.userKwh })
        assertFalse(deleted.suggestions.first().explanation.contains("weekend laundry"))
    }

    @Test fun handlesCrossMidnightUsageAndIgnoresFutureAndOldLogs() {
        val result = generator.generate(listOf(
            log("overnight", "heating_cooling", today.minusDays(1), 23, 120),
            log("future", "washing_machine", today.plusDays(1), 12, 60),
            log("old", "washing_machine", today.minusDays(40), 12, 60)
        ), today)
        assertEquals(1, result.historyLogCount)
        assertEquals(2, result.historyDayCount)
        // Repeating the two hourly slices must preserve the 3 kWh historical total.
        assertEquals(4.38, result.days[0].userKwh, 0.0001)
        assertTrue(result.days.all { it.userHourlyKwh.size == 24 })
    }

    @Test fun doesNotClaimSolarSurplusWhenUserDemandExceedsTheScenario() {
        val result = generator.generate(listOf(log("large", "heating_cooling", today, 11, 180, 10.0)), today)
        assertTrue(result.suggestions.last().title.contains("Stagger"))
        assertFalse(result.suggestions.last().explanation.contains("leaves about"))
    }

    @Test fun forecastsRemainDeterministicAndHandleDaylightSavingChange() {
        val day = LocalDate.of(2026, 10, 25)
        val logs = listOf(log("dst", "heating_cooling", day, 1, 240))
        val first = generator.generate(logs, day)
        assertEquals(first, generator.generate(logs, day))
        assertEquals(8.88, first.days.first().userKwh, 0.0001)
    }
}
