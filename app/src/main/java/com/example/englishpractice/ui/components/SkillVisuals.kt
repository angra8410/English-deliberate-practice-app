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
            accent = Color(0xFF8AE8FF),
            soft = Color(0xFF263D7F),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF4A41BC), Color(0xFF2866C9))
            )
        )

        SkillType.WRITING -> SkillTone(
            accent = Color(0xFFFFC95D),
            soft = Color(0xFF6A3E0C),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFFB95435), Color(0xFFF18A3A))
            )
        )

        SkillType.LISTENING -> SkillTone(
            accent = Color(0xFF85F5D0),
            soft = Color(0xFF155D63),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF0F9288), Color(0xFF31CDA7))
            )
        )

        SkillType.SPEAKING -> SkillTone(
            accent = Color(0xFFFF9EE8),
            soft = Color(0xFF72318B),
            gradient = Brush.linearGradient(
                colors = listOf(Color(0xFF7D2DD0), Color(0xFFFF4FD8))
            )
        )
    }
}
