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
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.AppViewModel
import com.example.englishpractice.ui.screens.activity.ActivityPlayerScreen
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
                    onStartPractice = { navController.navigate(AppDestination.Practice.route) }
                )
            }
            composable(AppDestination.Practice.route) {
                PracticeScreen(
                    state = uiState,
                    onSkillSelected = { skill ->
                        navController.navigate(AppDestination.ActivityPlayer.createRoute(skill))
                    }
                )
            }
            composable(AppDestination.Review.route) { ReviewScreen(state = uiState) }
            composable(AppDestination.Progress.route) { ProgressScreen(state = uiState) }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    state = uiState,
                    onSpeakingLocaleSelected = appViewModel::updateSpeakingLocale
                )
            }
            composable(
                route = AppDestination.ActivityPlayer.route,
                arguments = listOf(
                    navArgument(AppDestination.ActivityPlayer.skillArg) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val skill = backStackEntry.arguments
                    ?.getString(AppDestination.ActivityPlayer.skillArg)
                    ?.let { value -> runCatching { SkillType.valueOf(value) }.getOrNull() }

                ActivityPlayerScreen(
                    activity = skill?.let(appViewModel::getActivity),
                    lastAttempt = skill?.let { selectedSkill ->
                        uiState.recentAttempts.firstOrNull { attempt -> attempt.skill == selectedSkill }
                    },
                    selectedSpeakingLocaleTag = uiState.selectedSpeakingLocaleTag,
                    speakingCapability = uiState.speakingCapability,
                    listeningCapability = uiState.listeningCapability,
                    onSpeakingLocaleSelected = appViewModel::updateSpeakingLocale,
                    onSubmit = { answer, transcript ->
                        if (skill != null) {
                            appViewModel.submitActivity(skill, answer, transcript)
                        }
                    }
                )
            }
        }
    }
}
