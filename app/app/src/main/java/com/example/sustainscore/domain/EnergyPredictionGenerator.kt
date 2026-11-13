package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class EnergyPredictionDay(
    val date: LocalDate,
    val userHourlyKwh: List<Double>,
    val neighboursHourlyKwh: List<Double>,
    val sharedSolarHourlyKwh: List<Double>
) {
    val userKwh: Double get() = userHourlyKwh.sum()
    val neighboursKwh: Double get() = neighboursHourlyKwh.sum()
}

data class EnergySuggestion(val title: String, val explanation: String)

data class EnergyPrediction(
    val days: List<EnergyPredictionDay>,
    val historyLogCount: Int,
    val historyDayCount: Int,
    val suggestions: List<EnergySuggestion>
)

/** Local, deterministic demonstration. No trained model, meter feed or network request. */
class EnergyPredictionGenerator(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    // Illustrative average appliance power. The old sustainability weights are not kWh.
    private val applianceKw = mapOf(
        "lighting" to 0.01,
        "washing_machine" to 0.7,
        "computer_tv" to 0.12,
        "heating_cooling" to 1.5
    )

    fun generate(logs: List<ActivityLog>, today: LocalDate): EnergyPrediction {
        val historyStart = today.minusDays(27)
        val historyEnd = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        val relevantLogs = logs.filter {
            it.quantity.isFinite() && it.quantity > 0 && it.durationMinutes > 0 &&
                it.activityTypeId in applianceKw &&
                Instant.ofEpochMilli(it.startTimestampMillis).isBefore(historyEnd) &&
                Instant.ofEpochMilli(it.startTimestampMillis).plusSeconds(it.durationMinutes * 60L)
                    .isAfter(historyStart.atStartOfDay(zoneId).toInstant())
        }
        val byDay = mutableMapOf<LocalDate, DoubleArray>()
        relevantLogs.forEach { log ->
            var cursor = Instant.ofEpochMilli(log.startTimestampMillis)
            val end = minOf(cursor.plusSeconds(log.durationMinutes * 60L), historyEnd)
            cursor = maxOf(cursor, historyStart.atStartOfDay(zoneId).toInstant())
            while (cursor.isBefore(end)) {
                val local = cursor.atZone(zoneId)
                val nextHour = local.plusHours(1).withMinute(0).withSecond(0).withNano(0).toInstant()
                val sliceEnd = minOf(end, nextHour)
                val hours = (sliceEnd.toEpochMilli() - cursor.toEpochMilli()) / 3_600_000.0
                byDay.getOrPut(local.toLocalDate()) { DoubleArray(24) }[local.hour] +=
                    applianceKw.getValue(log.activityTypeId) * log.quantity * hours
                cursor = sliceEnd
            }
        }

        val overall = List(24) { hour ->
            if (byDay.isEmpty()) 0.0 else byDay.values.sumOf { it[hour] } / byDay.size
        }
        val days = (1L..7L).map { offset ->
            val date = today.plusDays(offset)
            val matches = byDay.filterKeys { it.dayOfWeek == date.dayOfWeek }.values
            val user = List(24) { hour ->
                val weekday = if (matches.isEmpty()) overall[hour] else matches.sumOf { it[hour] } / matches.size
                // Small assumed unlogged household load, plus the learned activity profile.
                0.12 + weekday * 0.7 + overall[hour] * 0.3
            }
            val weekend = date.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val dayFactor = listOf(0.92, 0.87, 0.94, 1.0, 1.08, 1.32, 1.22)[date.dayOfWeek.value - 1]
            val neighbours = List(24) { hour ->
                // Sum of four fictional neighbours. Stable across refreshes for a repeatable demo.
                val perHome = when (hour) {
                    in 0..5 -> 0.14
                    in 6..8 -> 0.44
                    in 9..15 -> if (weekend) 0.68 else 0.26
                    in 16..21 -> 0.75
                    else -> 0.24
                }
                perHome * 4 * dayFactor
            }
            val solar = List(24) { hour ->
                // Illustrative community solar, not live weather or a production forecast.
                val curve = listOf(0.0, 0.3, 1.0, 2.3, 3.6, 4.4, 4.6, 4.1, 3.0, 1.7, 0.6, 0.0)
                if (hour in 7..18) curve[hour - 7] else 0.0
            }
            EnergyPredictionDay(date, user, neighbours, solar)
        }
        return EnergyPrediction(days, relevantLogs.size, byDay.size, suggestions(relevantLogs, days))
    }

    private fun suggestions(logs: List<ActivityLog>, days: List<EnergyPredictionDay>): List<EnergySuggestion> {
        if (logs.isEmpty()) return listOf(EnergySuggestion(
            "Log a routine to personalise your plan",
            "There is no recent activity history yet. Your graph uses only an assumed 2.9 kWh daily background load. Log lighting, laundry or heating to get suggestions based on your routine."
        ), EnergySuggestion(
            "Explore a quieter community day",
            "The four sample neighbours have higher weekend demand. If your routine allows, a weekday around 11:00–14:00 could use more of the sample shared solar. This is a demo scenario, not measured availability."
        ))

        val result = mutableListOf<EnergySuggestion>()
        val laundry = logs.filter { it.activityTypeId == "washing_machine" }
        val morningPresence = logs.filter {
            it.activityTypeId in listOf("lighting", "computer_tv") &&
                Instant.ofEpochMilli(it.startTimestampMillis).atZone(zoneId).hour in 6..13
        }.map { Instant.ofEpochMilli(it.startTimestampMillis).atZone(zoneId).dayOfWeek }.toSet()
        val weekdayOptions = days.filter { it.date.dayOfWeek.value <= 5 }
        val candidates = weekdayOptions.filter { it.date.dayOfWeek in morningPresence }.ifEmpty { weekdayOptions }
        val quiet = candidates.minBy { it.neighboursKwh + it.userKwh }
        val peak = days.maxBy { it.neighboursKwh + it.userKwh }
        val dayLabel = quiet.date.format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale.ENGLISH))
        val peakLabel = peak.date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
        val presence = if (quiet.date.dayOfWeek in morningPresence) {
            "You logged lighting or screen use on ${quiet.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} mornings, so you may be home then. "
        } else "If someone is home then, "
        if (laundry.isNotEmpty()) {
            val weekendLaundry = laundry.any {
                Instant.ofEpochMilli(it.startTimestampMillis).atZone(zoneId).dayOfWeek.value >= 6
            }
            val source = if (weekendLaundry) "You logged weekend laundry. " else "Your history includes ${laundry.size} washing machine session(s). "
            result += EnergySuggestion(
                "Try laundry on $dayLabel",
                source + "The combined forecast is highest on $peakLabel. " + presence +
                    "Try one cycle at 11:00–14:00 on $dayLabel, when sample neighbour demand is lower and shared solar is available. Confirm that this fits your routine."
            )
        } else {
            result += EnergySuggestion("Keep flexible tasks for $dayLabel",
                "Your forecast and the four sample neighbours show the busiest combined day on $peakLabel. " +
                    presence + "Consider scheduling a flexible appliance at 11:00–14:00 on $dayLabel instead.")
        }

        val yourPeak = days.maxBy { it.userKwh }
        val eveningLogs = logs.filter {
            Instant.ofEpochMilli(it.startTimestampMillis).atZone(zoneId).hour in 17..22
        }
        if (eveningLogs.isNotEmpty()) {
            result += EnergySuggestion("Avoid stacking evening appliances",
                "${eveningLogs.size} of your recent logs start between 17:00 and 23:00. The sample neighbours also peak in the evening, while sample solar falls to zero after 18:00. Keep essential lighting and comfort, but move a flexible load to midday if convenient.")
        } else {
            result += EnergySuggestion("Plan around your busiest day",
                "Your highest predicted daily use is ${yourPeak.date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))} at ${format(yourPeak.userKwh)} kWh. Spread flexible tasks across quieter days rather than starting them together. Your graph estimates logged appliances plus an assumed background load.")
        }
        val solarDay = days.minBy { day ->
            (11..13).sumOf { hour -> day.userHourlyKwh[hour] + day.neighboursHourlyKwh[hour] - day.sharedSolarHourlyKwh[hour] }
        }
        val surplus = (11..13).sumOf { hour ->
            (solarDay.sharedSolarHourlyKwh[hour] - solarDay.userHourlyKwh[hour] - solarDay.neighboursHourlyKwh[hour]).coerceAtLeast(0.0)
        }
        result += if (surplus > 0.1) EnergySuggestion(
            "Coordinate a shared solar window",
            "On ${solarDay.date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))}, the sample solar profile leaves about ${format(surplus)} kWh after predicted group demand between 11:00 and 14:00. Agree on staggered appliance starts with neighbours. This illustrates a sharing opportunity; it is not a confirmed allocation or a saving."
        ) else EnergySuggestion(
            "Stagger loads during the solar window",
            "Your updated demand leaves little spare sample solar between 11:00 and 14:00. Agree on separate appliance start times with neighbours, and keep essential loads running. The demo cannot guarantee that the community can avoid grid imports."
        )
        return result
    }

    private fun format(value: Double) = String.format(Locale.ENGLISH, "%.1f", value)
}
