package com.example.englishpractice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF4FD8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF5A2CD2),
    onPrimaryContainer = Color(0xFFF8F2FF),
    secondary = Color(0xFF7FE5FF),
    onSecondary = Color(0xFF10203C),
    secondaryContainer = Color(0xFF2D3A8E),
    onSecondaryContainer = Color(0xFFEAF8FF),
    tertiary = Color(0xFFFFC857),
    onTertiary = Color(0xFF372300),
    tertiaryContainer = Color(0xFF5F3B00),
    onTertiaryContainer = Color(0xFFFFF2CC),
    error = Color(0xFFFF7B9F),
    onError = Color(0xFF3F0016),
    errorContainer = Color(0xFF702043),
    onErrorContainer = Color(0xFFFFE0E8),
    background = Color(0xFF22105B),
    onBackground = Color(0xFFF7F2FF),
    surface = Color(0xFF45209A),
    onSurface = Color(0xFFF7F2FF),
    surfaceVariant = Color(0xFF5C34B7),
    onSurfaceVariant = Color(0xFFE2D8FF),
    outline = Color(0xFFA793F7),
    outlineVariant = Color(0xFF6F4BC6),
    surfaceContainer = Color(0xFF4F28A8),
    surfaceContainerLow = Color(0xFF3D1D89),
    surfaceContainerHighest = Color(0xFF633DC6)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF62E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6B32E1),
    onPrimaryContainer = Color(0xFFF8F2FF),
    secondary = Color(0xFF8AE8FF),
    onSecondary = Color(0xFF10203C),
    secondaryContainer = Color(0xFF24357D),
    onSecondaryContainer = Color(0xFFEAF8FF),
    tertiary = Color(0xFFFFD26B),
    onTertiary = Color(0xFF3A2500),
    tertiaryContainer = Color(0xFF654100),
    onTertiaryContainer = Color(0xFFFFF2CC),
    error = Color(0xFFFF91AF),
    onError = Color(0xFF4B001D),
    errorContainer = Color(0xFF772745),
    onErrorContainer = Color(0xFFFFE0E8),
    background = Color(0xFF160A3D),
    onBackground = Color(0xFFF7F2FF),
    surface = Color(0xFF2D146B),
    onSurface = Color(0xFFF7F2FF),
    surfaceVariant = Color(0xFF3D2188),
    onSurfaceVariant = Color(0xFFD7CBFF),
    outline = Color(0xFF9E8AEF),
    outlineVariant = Color(0xFF51309D),
    surfaceContainer = Color(0xFF341977),
    surfaceContainerLow = Color(0xFF28115F),
    surfaceContainerHighest = Color(0xFF452391)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.6.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.4.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

@Composable
fun EnglishPracticeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
