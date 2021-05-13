package com.benoitletondor.pixelminimalwatchfacecompanion.view.main.subviews

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.benoitletondor.pixelminimalwatchfacecompanion.R
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.AppMaterialTheme
import com.benoitletondor.pixelminimalwatchfacecompanion.view.main.MainViewModel

@Composable
fun Error(navController: NavController, state: MainViewModel.State.Error, viewModel: MainViewModel) {
    ErrorLayout(
        errorMessage = state.error.localizedMessage ?: "",
        onRetryButtonClicked = {
            viewModel.retryPremiumStatusCheck()
        },
    )
}

@Composable
private fun ErrorLayout(errorMessage: String, onRetryButtonClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 26.dp)
            .fillMaxHeight(fraction = 0.9f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.premium_error_title),
            color = MaterialTheme.colors.onBackground,
            fontSize = 22.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.premium_error, errorMessage),
            color = MaterialTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(30.dp))
        Button(onClick = onRetryButtonClicked) {
            Text(stringResource(R.string.error_retry_cta))
        }
    }
}

@Composable
@Preview(showSystemUi = true)
private fun Preview() {
    AppMaterialTheme {
        ErrorLayout(
            errorMessage = "Preview error message",
            onRetryButtonClicked = {},
        )
    }
}