package com.benoitletondor.pixelminimalwatchfacecompanion.view.main.subviews

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.benoitletondor.pixelminimalwatchfacecompanion.view.main.MainViewModel

@Composable
fun NotPremium(navController: NavController, state: MainViewModel.State.NotPremium, viewModel: MainViewModel) {
    Text(text = "Not premium")
}