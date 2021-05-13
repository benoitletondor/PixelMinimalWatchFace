package com.benoitletondor.pixelminimalwatchfacecompanion.view.main.subviews

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.benoitletondor.pixelminimalwatchfacecompanion.view.main.MainViewModel

@Composable
fun Error(navController: NavController, state: MainViewModel.State.Error, viewModel: MainViewModel) {
    Text(text = "Error: ${state.error.localizedMessage}")
}