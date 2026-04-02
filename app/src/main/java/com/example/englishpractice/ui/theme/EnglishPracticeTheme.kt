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
    primary = Color(0xFF1D5C57),
    onPrimary = Color(0xFFF7F5EF),
    primaryContainer = Color(0xFFCDEBE3),
    onPrimaryContainer = Color(0xFF103A37),
    secondary = Color(0xFFB46A2C),
    onSecondary = Color(0xFFFFF8F1),
    secondaryContainer = Color(0xFFF6DFC8),
    onSecondaryContainer = Color(0xFF5D3612),
    tertiary = Color(0xFF4E6286),
    onTertiary = Color(0xFFF7F7FC),
    tertiaryContainer = Color(0xFFDCE5F7),
    onTertiaryContainer = Color(0xFF263653),
    error = Color(0xFFA24632),
    onError = Color(0xFFFFF8F6),
    errorContainer = Color(0xFFF6D6CF),
    onErrorContainer = Color(0xFF5A2218),
    background = Color(0xFFF4EFE6),
    onBackground = Color(0xFF1E1B18),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF221F1C),
    surfaceVariant = Color(0xFFE5DDD0),
    onSurfaceVariant = Color(0xFF5C554D),
    outline = Color(0xFF9C9389),
    outlineVariant = Color(0xFFD1C8BC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CCDC1),
    onPrimary = Color(0xFF0C3431),
    primaryContainer = Color(0xFF174744),
    onPrimaryContainer = Color(0xFFCDEBE3),
    secondary = Color(0xFFF0BE8A),
    onSecondary = Color(0xFF53300F),
    secondaryContainer = Color(0xFF70411A),
    onSecondaryContainer = Color(0xFFF6DFC8),
    tertiary = Color(0xFFB9C7E4),
    onTertiary = Color(0xFF22314E),
    tertiaryContainer = Color(0xFF37476A),
    onTertiaryContainer = Color(0xFFDCE5F7),
    error = Color(0xFFE8B4A8),
    onError = Color(0xFF5F1F13),
    errorContainer = Color(0xFF7C3323),
    onErrorContainer = Color(0xFFF6D6CF),
    background = Color(0xFF181513),
    onBackground = Color(0xFFF3EEE8),
    surface = Color(0xFF211D1A),
    onSurface = Color(0xFFF3EEE8),
    surfaceVariant = Color(0xFF4B443D),
    onSurfaceVariant = Color(0xFFD0C7BB),
    outline = Color(0xFF9C9389),
    outlineVariant = Color(0xFF4B443D)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 28.sp
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
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
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
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
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
