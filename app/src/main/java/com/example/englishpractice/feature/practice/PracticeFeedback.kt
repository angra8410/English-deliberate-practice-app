package com.example.englishpractice.feature.practice

data class PracticeFeedback(
    val score: Int,
    val feedback: List<String>,
    val weakTags: List<String>
)
