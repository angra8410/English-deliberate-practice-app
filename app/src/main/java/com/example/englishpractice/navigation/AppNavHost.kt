package com.example.englishpractice.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.englishpractice.ui.app.AppViewModel
import com.example.englishpractice.ui.screens.activity.ActivityPlayerScreen
import com.example.englishpractice.ui.screens.browse.ContentBrowserScreen
import com.example.englishpractice.ui.screens.home.HomeScreen
import com.example.englishpractice.ui.screens.progress.ProgressScreen
import com.example.englishpractice.ui.screens.practice.PracticeScreen
import com.example.englishpractice.ui.screens.review.ReviewScreen
import com.example.englishpractice.ui.screens.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel()
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val items = listOf(
        AppDestination.Home,
        AppDestination.Practice,
        AppDestination.Review,
        AppDestination.Progress,
        AppDestination.Settings
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTopLevelDestination = items.any { destination -> destination.route == currentRoute }
    val topBarTitle = when (currentRoute) {
        AppDestination.Browse.route -> AppDestination.Browse.label
        AppDestination.ActivityPlayer.route -> AppDestination.ActivityPlayer.label
        else -> items.firstOrNull { destination -> destination.route == currentRoute }?.label
            ?: "English Deliberate Practice"
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (!isTopLevelDestination) {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            if (isTopLevelDestination) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shadowElevation = 16.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        )
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            items.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = {
                                        if (destination == AppDestination.Home) {
                                            val popped = navController.popBackStack(
                                                AppDestination.Home.route,
                                                inclusive = false
                                            )
                                            if (!popped) {
                                                navController.navigate(AppDestination.Home.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                }
                                            }
                                        } else {
                                            navController.navigate(destination.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = destinationIcon(destination),
                                            contentDescription = destination.label
                                        )
                                    },
                                    label = { Text(destination.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    state = uiState,
                    onPilotLevelSelected = appViewModel::updatePilotLevel,
                    onStartPractice = { navController.navigate(AppDestination.Practice.route) },
                    onBrowseContent = { navController.navigate(AppDestination.Browse.route) },
                    onResumeReview = { activityId ->
                        navController.navigate(AppDestination.ActivityPlayer.createRoute(activityId))
                    }
                )
            }
            composable(AppDestination.Practice.route) {
                PracticeScreen(
                    state = uiState,
                    onSkillSelected = { skill ->
                        appViewModel.getFirstActivityForSkill(skill)?.id?.let { activityId ->
                            navController.navigate(AppDestination.ActivityPlayer.createRoute(activityId))
                        }
                    },
                    onBrowseContent = { navController.navigate(AppDestination.Browse.route) }
                )
            }
            composable(AppDestination.Browse.route) {
                ContentBrowserScreen(
                    state = uiState,
                    onActivitySelected = { activityId ->
                        navController.navigate(AppDestination.ActivityPlayer.createRoute(activityId))
                    }
                )
            }
            composable(AppDestination.Review.route) {
                ReviewScreen(
                    state = uiState,
                    onOpenReviewActivity = { activityId ->
                        navController.navigate(AppDestination.ActivityPlayer.createRoute(activityId))
                    }
                )
            }
            composable(AppDestination.Progress.route) { ProgressScreen(state = uiState) }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    state = uiState,
                    onPilotLevelSelected = appViewModel::updatePilotLevel,
                    onSpeakingLocaleSelected = appViewModel::updateSpeakingLocale
                )
            }
            composable(
                route = AppDestination.ActivityPlayer.route,
                arguments = listOf(
                    navArgument(AppDestination.ActivityPlayer.activityIdArg) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments
                    ?.getString(AppDestination.ActivityPlayer.activityIdArg)
                val activity = activityId?.let(appViewModel::getActivity)

                ActivityPlayerScreen(
                    activity = activity,
                    availableListeningActivities = uiState.activityCatalog.filter { item ->
                        item.skill == com.example.englishpractice.domain.model.SkillType.LISTENING
                    },
                    lastAttempt = activityId?.let { selectedActivityId ->
                        uiState.recentAttempts.firstOrNull { attempt ->
                            attempt.activityId == selectedActivityId
                        }
                    },
                    selectedSpeakingLocaleTag = uiState.selectedSpeakingLocaleTag,
                    speakingCapability = uiState.speakingCapability,
                    listeningCapability = uiState.listeningCapability,
                    onSpeakingLocaleSelected = appViewModel::updateSpeakingLocale,
                    onListeningActivitySelected = { selectedListeningActivityId ->
                        navController.navigate(
                            AppDestination.ActivityPlayer.createRoute(selectedListeningActivityId)
                        )
                    },
                    onSubmit = { answer, transcript ->
                        if (activityId != null) {
                            appViewModel.submitActivity(activityId, answer, transcript)
                        }
                    }
                )
            }
        }
    }
}

private fun destinationIcon(destination: AppDestination) = when (destination) {
    AppDestination.Home -> Icons.Rounded.Home
    AppDestination.Practice -> Icons.AutoMirrored.Rounded.MenuBook
    AppDestination.Review -> Icons.Rounded.Refresh
    AppDestination.Progress -> Icons.Rounded.BarChart
    AppDestination.Settings -> Icons.Rounded.Settings
    else -> Icons.Rounded.Home
}
