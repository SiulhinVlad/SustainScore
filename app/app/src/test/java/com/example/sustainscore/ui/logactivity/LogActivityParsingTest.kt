package com.example.sustainscore.ui.logactivity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogActivityParsingTest {

    @Test
    fun parseDurationMinutesTreatsSingleDigitHourAsWholeHours() {
        assertEquals(420, parseDurationMinutes("7"))
    }

    @Test
    fun parseDurationMinutesStillSupportsHourMinuteFormat() {
        assertEquals(150, parseDurationMinutes("02:30"))
    }

    @Test
    fun parseDurationMinutesRejectsSingleNumberTwentyFourOrMore() {
        assertEquals(null, parseDurationMinutes("24"))
    }

    @Test
    fun isValidTimeOfDayAcceptsSingleHourInput() {
        assertTrue(isValidTimeOfDay("7"))
        assertEquals(7 to 0, parseTimeOfDay("7"))
    }

    @Test
    fun isValidTimeOfDayStillAcceptsHourMinuteFormat() {
        assertTrue(isValidTimeOfDay("18:45"))
        assertEquals(18 to 45, parseTimeOfDay("18:45"))
    }

    @Test
    fun isValidTimeOfDayRejectsSingleNumberTwentyFourOrMore() {
        assertFalse(isValidTimeOfDay("24"))
    }
}
