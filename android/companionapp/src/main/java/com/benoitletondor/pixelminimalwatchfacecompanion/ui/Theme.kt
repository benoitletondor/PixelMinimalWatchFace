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
            primary = primaryBlue,
            background = Color.Black,
            surface = Color.Black,
            onPrimary = Color.White,
            onBackground = Color.White,
            error = Color.Red,
            onSurface = Color.White,
            secondary = primaryRed,
            onSecondary = Color.White,
            isLight = false,
            onError = Color.White,
            primaryVariant = primaryBlue,
            secondaryVariant = primaryRed,
        ),
        content = content,
    )
}