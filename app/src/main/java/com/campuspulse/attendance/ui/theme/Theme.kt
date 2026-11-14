package com.campuspulse.attendance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1E40AF)
val SecondaryBlue = Color(0xFF2563EB)
val SurfaceLight = Color(0xFFF8FAFC)
val CardBorder = Color(0xFFE2E8F0)

val PresentGreen = Color(0xFF15803D)
val PresentGreenBg = Color(0xFFDCFCE7)

val LateAmber = Color(0xFFB45309)
val LateAmberBg = Color(0xFFFEF3C7)

val AbsentRed = Color(0xFFB91C1C)
val AbsentRedBg = Color(0xFFFEE2E2)

val DismissedIndigo = Color(0xFF4338CA)
val DismissedIndigoBg = Color(0xFFE0E7FF)

val UnmarkedSlate = Color(0xFF475569)
val UnmarkedSlateBg = Color(0xFFF1F5F9)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    background = SurfaceLight,
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun CampusPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
