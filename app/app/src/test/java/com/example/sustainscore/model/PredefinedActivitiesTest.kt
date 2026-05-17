package com.example.sustainscore.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredefinedActivitiesTest {

    @Test
    fun containsFirstVersionActivities() {
        val activityIds = PredefinedActivities.all.map { it.id }

        assertEquals(4, PredefinedActivities.all.size)
        assertTrue(activityIds.contains("lighting"))
        assertTrue(activityIds.contains("washing_machine"))
        assertTrue(activityIds.contains("computer_tv"))
        assertTrue(activityIds.contains("heating_cooling"))
    }

    @Test
    fun activityIdsAreUnique() {
        val activityIds = PredefinedActivities.all.map { it.id }

        assertEquals(activityIds.size, activityIds.toSet().size)
    }

    @Test
    fun findsActivityById() {
        val activity = PredefinedActivities.findById("lighting")

        assertNotNull(activity)
        assertEquals("Lighting", activity?.name)
    }
}
