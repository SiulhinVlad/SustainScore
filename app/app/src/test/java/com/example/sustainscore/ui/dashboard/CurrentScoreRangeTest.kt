package com.example.sustainscore.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentScoreRangeTest {

    @Test
    fun mapsScoresToGoodRange() {
        assertEquals(CurrentScoreRange.Good, currentScoreRange(80.0))
        assertEquals(CurrentScoreRange.Good, currentScoreRange(92.5))
    }

    @Test
    fun mapsScoresToOkRange() {
        assertEquals(CurrentScoreRange.Ok, currentScoreRange(60.0))
        assertEquals(CurrentScoreRange.Ok, currentScoreRange(79.9))
    }

    @Test
    fun mapsScoresToBadRange() {
        assertEquals(CurrentScoreRange.Bad, currentScoreRange(40.0))
        assertEquals(CurrentScoreRange.Bad, currentScoreRange(59.9))
    }

    @Test
    fun mapsScoresToDangerRange() {
        assertEquals(CurrentScoreRange.Danger, currentScoreRange(39.9))
        assertEquals(CurrentScoreRange.Danger, currentScoreRange(5.0))
    }
}
