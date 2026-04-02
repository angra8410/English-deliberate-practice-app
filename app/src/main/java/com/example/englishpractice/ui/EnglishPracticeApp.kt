package com.example.englishpractice.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.example.englishpractice.navigation.AppNavHost
import com.example.englishpractice.ui.theme.EnglishPracticeTheme

@Composable
fun EnglishPracticeApp() {
    EnglishPracticeTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppNavHost()
        }
    }
}
