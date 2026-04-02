package com.example.englishpractice.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = {
                    if (!isTopLevelDestination) {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
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
                            icon = {},
                            label = { Text(destination.label) }
                        )
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
                    onBrowseContent = { navController.navigate(AppDestination.Browse.route) }
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
            composable(AppDestination.Review.route) { ReviewScreen(state = uiState) }
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
                    lastAttempt = activityId?.let { selectedActivityId ->
                        uiState.recentAttempts.firstOrNull { attempt ->
                            attempt.activityId == selectedActivityId
                        }
                    },
                    selectedSpeakingLocaleTag = uiState.selectedSpeakingLocaleTag,
                    speakingCapability = uiState.speakingCapability,
                    listeningCapability = uiState.listeningCapability,
                    onSpeakingLocaleSelected = appViewModel::updateSpeakingLocale,
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
