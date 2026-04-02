package com.example.englishpractice.feature.review

object ReviewScheduler {
    fun nextIntervalDays(previousIntervalDays: Int, wasSuccessful: Boolean): Int {
        return when {
            !wasSuccessful -> 1
            previousIntervalDays <= 0 -> 1
            previousIntervalDays == 1 -> 3
            previousIntervalDays == 3 -> 7
            previousIntervalDays == 7 -> 14
            else -> previousIntervalDays * 2
        }
    }
}
