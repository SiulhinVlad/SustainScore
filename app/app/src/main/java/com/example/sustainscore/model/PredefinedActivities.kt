package com.example.sustainscore.model

object PredefinedActivities {
    val all: List<ActivityType> = listOf(
        ActivityType(
            id = "lighting",
            name = "Lighting",
            unit = ActivityUnit.COUNT,
            weight = 2.0,
            recommendedLimit = 2.0
        ),
        ActivityType(
            id = "washing_machine",
            name = "Washing Machine",
            unit = ActivityUnit.HOURS,
            weight = 12.0,
            recommendedLimit = 1.0
        ),
        ActivityType(
            id = "computer_tv",
            name = "Computer or TV",
            unit = ActivityUnit.HOURS,
            weight = 4.0,
            recommendedLimit = 2.0
        ),
        ActivityType(
            id = "heating_cooling",
            name = "Heating or Cooling",
            unit = ActivityUnit.HOURS,
            weight = 8.0,
            recommendedLimit = 3.0
        )
    )

    fun findById(id: String): ActivityType? = all.find { it.id == id }
}
