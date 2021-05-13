package com.benoitletondor.pixelminimalwatchfacecompanion.view.main.subviews

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benoitletondor.pixelminimalwatchfacecompanion.R
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.AppMaterialTheme
import com.benoitletondor.pixelminimalwatchfacecompanion.view.main.MainViewModel

@Composable
fun Loading(navController: NavController, viewModel: MainViewModel) {
    LoadingLayout()
}

@Composable
private fun LoadingLayout() {
    Column(
        modifier = Modifier
            .fillMaxHeight(fraction = 0.9f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.loading_premium_status),
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
@Preview(showSystemUi = true)
private fun Preview() {
    AppMaterialTheme {
        LoadingLayout()
    }
}