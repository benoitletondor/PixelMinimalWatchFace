package com.benoitletondor.pixelminimalwatchfacecompanion.ui

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val primaryBlue = Color(0xFF5484f8)
val primaryRed = Color(0xFFda482f)

@Composable
fun AppMaterialTheme(content: @Composable () -> Unit) {
    return MaterialTheme(
        colors = Colors(
            primary = primaryRed,
            primaryVariant = primaryRed,
            background = Color.Black,
            surface = Color.Black,
            onPrimary = Color.White,
            onBackground = Color.White,
            error = Color.Red,
            onSurface = Color.White,
            secondary = primaryBlue,
            secondaryVariant = primaryBlue,
            onSecondary = Color.White,
            isLight = false,
            onError = Color.White,
        ),
        content = content,
    )
}