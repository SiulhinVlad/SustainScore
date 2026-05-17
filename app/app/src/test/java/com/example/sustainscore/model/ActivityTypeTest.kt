package com.example.sustainscore

import com.example.sustainscore.model.ActivityType
import com.example.sustainscore.model.ActivityUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityTypeTest {

    @Test
    fun createsActivityTypeWithExpectedValues() {
        val activityType = ActivityType(
            id = "lighting",
            name = "Lighting",
            unit = ActivityUnit.COUNT,
            weight = 2.0,
            recommendedLimit = 3.0
        )

        assertEquals("lighting", activityType.id)
        assertEquals("Lighting", activityType.name)
        assertEquals(ActivityUnit.COUNT, activityType.unit)
        assertEquals(2.0, activityType.weight, 0.001)
        assertEquals(3.0, activityType.recommendedLimit, 0.001)
    }
}