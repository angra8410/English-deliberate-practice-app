package com.example.englishpractice.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.englishpractice.domain.model.SkillType

data class SkillTone(
    val accent: Color,
    val soft: Color,
    val gradient: Brush
)

@Composable
fun skillTone(skill: SkillType): SkillTone {
    return when (skill) {
        SkillType.READING -> SkillTone(
            accent = Color(0xFF3B5F8B),
            soft = Color(0xFFE0E9F8),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFFEAF1FB), Color(0xFFD6E4F7))
            )
        )

        SkillType.WRITING -> SkillTone(
            accent = Color(0xFF9A5B28),
            soft = Color(0xFFF6E2D2),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFFF9EEE5), Color(0xFFF2DBC8))
            )
        )

        SkillType.LISTENING -> SkillTone(
            accent = Color(0xFF1D6A63),
            soft = Color(0xFFD7EEE8),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFFE7F7F2), Color(0xFFD0ECE4))
            )
        )

        SkillType.SPEAKING -> SkillTone(
            accent = Color(0xFF8B4C63),
            soft = Color(0xFFF4DDE6),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFFF8EBF0), Color(0xFFF1D4DF))
            )
        )
    }
}
