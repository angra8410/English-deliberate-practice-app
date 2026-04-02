package com.example.englishpractice.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.englishpractice.navigation.AppNavHost

@Composable
fun EnglishPracticeApp() {
    MaterialTheme {
        AppNavHost()
    }
}
