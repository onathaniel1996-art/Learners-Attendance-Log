package com.campuspulse.attendance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Google Stitch "Modernist Campus Utility" Design System Tokens
val StitchSurface = Color(0xFFF8F9FF)
val StitchSurfaceLowest = Color(0xFFFFFFFF)
val StitchSurfaceLow = Color(0xFFEFF4FF)
val StitchSurfaceContainer = Color(0xFFE5EEFF)
val StitchSurfaceHigh = Color(0xFFDCE9FF)
val StitchSurfaceHighest = Color(0xFFD3E4FE)

val StitchOnSurface = Color(0xFF0B1C30)
val StitchOnSurfaceVariant = Color(0xFF464554)
val StitchOutline = Color(0xFF777586)
val StitchOutlineVariant = Color(0xFFC7C4D7)

val StitchPrimary = Color(0xFF2A14B4)
val StitchPrimaryContainer = Color(0xFF4338CA)
val StitchPrimaryFixed = Color(0xFFE3DFFF)
val StitchOnPrimaryFixed = Color(0xFF100069)

val StitchSecondary = Color(0xFF3755C3)
val StitchSecondaryFixed = Color(0xFFDDE1FF)
val StitchOnSecondaryFixed = Color(0xFF001453)

val StitchTertiary = Color(0xFF00451C)
val StitchTertiaryContainer = Color(0xFF005F29)
val StitchTertiaryFixed = Color(0xFF95F8A7)
val StitchTertiarySoftBg = Color(0xFFDDFCE4)
val StitchOnTertiaryFixed = Color(0xFF00210A)

val StitchError = Color(0xFFBA1A1A)
val StitchErrorContainer = Color(0xFFFFDAD6)
val StitchOnErrorContainer = Color(0xFF93000A)

// Semantic Aliases
val PrimaryBlue = StitchPrimary
val SecondaryBlue = StitchSecondary
val SurfaceLight = StitchSurface
val CardBorder = Color(0xFFE2E8F0)

val PresentGreen = StitchTertiary
val PresentGreenBg = StitchTertiarySoftBg

val LateAmber = StitchSecondary
val LateAmberBg = StitchSurfaceHigh

val AbsentRed = StitchError
val AbsentRedBg = StitchErrorContainer

val DismissedIndigo = StitchPrimaryContainer
val DismissedIndigoBg = StitchPrimaryFixed

val UnmarkedSlate = StitchOnSurfaceVariant
val UnmarkedSlateBg = StitchSurfaceContainer

private val StitchLightColors = lightColorScheme(
    primary = StitchPrimary,
    onPrimary = Color.White,
    primaryContainer = StitchPrimaryFixed,
    onPrimaryContainer = StitchOnPrimaryFixed,
    secondary = StitchSecondary,
    onSecondary = Color.White,
    secondaryContainer = StitchSecondaryFixed,
    onSecondaryContainer = StitchOnSecondaryFixed,
    tertiary = StitchTertiary,
    onTertiary = Color.White,
    tertiaryContainer = StitchTertiaryFixed,
    onTertiaryContainer = StitchOnTertiaryFixed,
    error = StitchError,
    onError = Color.White,
    errorContainer = StitchErrorContainer,
    onErrorContainer = StitchOnErrorContainer,
    background = StitchSurface,
    onBackground = StitchOnSurface,
    surface = StitchSurfaceLowest,
    onSurface = StitchOnSurface,
    surfaceVariant = StitchSurfaceContainer,
    onSurfaceVariant = StitchOnSurfaceVariant,
    outline = StitchOutline,
    outlineVariant = StitchOutlineVariant
)

@Composable
fun CampusPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StitchLightColors,
        content = content
    )
}
