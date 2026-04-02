package com.example.englishpractice.navigation

import com.example.englishpractice.domain.model.SkillType

sealed class AppDestination(val route: String, val label: String) {
    data object Home : AppDestination("home", "Home")
    data object Practice : AppDestination("practice", "Practice")
    data object Review : AppDestination("review", "Review")
    data object Progress : AppDestination("progress", "Progress")
    data object Settings : AppDestination("settings", "Settings")

    data object ActivityPlayer : AppDestination("activity/{skill}", "Activity Player") {
        const val skillArg = "skill"

        fun createRoute(skill: SkillType): String {
            return "activity/${skill.name}"
        }
    }
}
