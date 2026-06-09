package com.serenare.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SerenareColors {
    val Background = Color(0xFF0A0A0F)
    val Surface = Color(0xFF14141C)
    val Crisis = Color(0xFFE8A020)
    val Relax = Color(0xFF4A7C6F)
    val Focus = Color(0xFF2C5F8A)
    val Sleep = Color(0xFF3D3060)
    val TextPrimary = Color(0xFFF0F0F5)
    val TextSecondary = Color(0xFF9090A8)
    val Danger = Color(0xFFC84646)
}

@Composable
fun SerenareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = SerenareColors.Background,
            surface = SerenareColors.Surface,
            primary = SerenareColors.Relax,
            secondary = SerenareColors.Focus,
            tertiary = SerenareColors.Crisis,
            onBackground = SerenareColors.TextPrimary,
            onSurface = SerenareColors.TextPrimary,
            onPrimary = SerenareColors.TextPrimary
        ),
        typography = MaterialTheme.typography,
        content = content
    )
}
