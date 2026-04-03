package com.example.englishpractice.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object Home : AppDestination("home", "Explore")
    data object Practice : AppDestination("practice", "Learn")
    data object Browse : AppDestination("browse", "Browse")
    data object Review : AppDestination("review", "Review")
    data object Progress : AppDestination("progress", "Progress")
    data object Settings : AppDestination("settings", "Settings")

    data object ActivityPlayer : AppDestination("activity/{activityId}", "Activity Player") {
        const val activityIdArg = "activityId"

        fun createRoute(activityId: String): String {
            return "activity/$activityId"
        }
    }
}
