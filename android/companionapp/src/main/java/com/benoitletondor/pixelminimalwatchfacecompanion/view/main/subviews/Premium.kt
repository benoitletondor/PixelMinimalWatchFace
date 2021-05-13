package com.benoitletondor.pixelminimalwatchfacecompanion.view.main.subviews

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benoitletondor.pixelminimalwatchfacecompanion.R
import com.benoitletondor.pixelminimalwatchfacecompanion.sync.Sync
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.AppMaterialTheme
import com.benoitletondor.pixelminimalwatchfacecompanion.view.main.MainViewModel
import java.lang.Exception
import java.util.*

@Composable
fun Premium(state: MainViewModel.State.Premium, viewModel: MainViewModel) {
    PremiumLayout(
        appInstalledStatus = state.appInstalledStatus,
        watchFaceInstallButtonPressed = {
            viewModel.onInstallWatchFaceButtonPressed()
        },
        syncPremiumStatusButtonPressed = {
            viewModel.triggerSync()
        },
        donateButtonPressed = {
            viewModel.onDonateButtonPressed()
        },
    )
}

@Composable
private fun PremiumLayout(
    appInstalledStatus: MainViewModel.AppInstalledStatus,
    watchFaceInstallButtonPressed: () -> Unit,
    syncPremiumStatusButtonPressed: () -> Unit,
    donateButtonPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.premium_status),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(when(appInstalledStatus) {
                is MainViewModel.AppInstalledStatus.Result -> when(appInstalledStatus.wearableStatus) {
                    Sync.WearableStatus.AvailableAppInstalled -> R.string.premium_status_installed
                    Sync.WearableStatus.AvailableAppNotInstalled -> R.string.premium_status_not_installed
                    is Sync.WearableStatus.Error, Sync.WearableStatus.NotAvailable -> R.string.premium_status_unknown
                }
                MainViewModel.AppInstalledStatus.Unknown -> R.string.premium_status_unknown
                MainViewModel.AppInstalledStatus.Verifying -> R.string.premium_status_loading
            }),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(30.dp))

        when(appInstalledStatus) {
            is MainViewModel.AppInstalledStatus.Result -> when(appInstalledStatus.wearableStatus) {
                Sync.WearableStatus.AvailableAppInstalled -> {
                    SyncPremiumStatusButton(syncPremiumStatusButtonPressed)
                }
                Sync.WearableStatus.AvailableAppNotInstalled -> {
                    Button(
                        onClick = watchFaceInstallButtonPressed,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(text = stringResource(R.string.premium_install_cta).toUpperCase(Locale.getDefault()))
                    }
                }
                is Sync.WearableStatus.Error, Sync.WearableStatus.NotAvailable -> {
                    SyncPremiumStatusButton(syncPremiumStatusButtonPressed)
                }
            }
            MainViewModel.AppInstalledStatus.Unknown -> {
                SyncPremiumStatusButton(syncPremiumStatusButtonPressed)
            }
            MainViewModel.AppInstalledStatus.Verifying -> {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(R.string.premium_status_thanks),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(R.string.donation_description),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = donateButtonPressed,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = Color.Black,
            ),
        ) {
            Text(text = stringResource(R.string.donation_cta).toUpperCase(Locale.getDefault()))
        }
    }
}

@Composable
private fun SyncPremiumStatusButton(syncPremiumStatusButtonPressed: () -> Unit) {
    Button(onClick = syncPremiumStatusButtonPressed) {
        Text(text = stringResource(R.string.premium_sync_cta).toUpperCase(Locale.getDefault()))
    }
}

@Composable
@Preview(showSystemUi = true, name = "Watch face not installed")
private fun PreviewNotInstalled() {
    Preview(MainViewModel.AppInstalledStatus.Result(Sync.WearableStatus.AvailableAppNotInstalled))
}

@Composable
@Preview(showSystemUi = true, name = "Watch face installed")
private fun PreviewInstalled() {
    Preview(MainViewModel.AppInstalledStatus.Result(Sync.WearableStatus.AvailableAppInstalled))
}

@Composable
@Preview(showSystemUi = true, name = "Watch face unvailable")
private fun PreviewUnavailable() {
    Preview(MainViewModel.AppInstalledStatus.Result(Sync.WearableStatus.NotAvailable))
}

@Composable
@Preview(showSystemUi = true, name = "Watch face error")
private fun PreviewError() {
    Preview(MainViewModel.AppInstalledStatus.Result(Sync.WearableStatus.Error(Exception("Error"))))
}

@Composable
@Preview(showSystemUi = true, name = "Watch face status unknown")
private fun PreviewUnknown() {
    Preview(MainViewModel.AppInstalledStatus.Unknown)
}

@Composable
@Preview(showSystemUi = true, name = "Watch face status verifying")
private fun PreviewVerifying() {
    Preview(MainViewModel.AppInstalledStatus.Verifying)
}

@Composable
private fun Preview(appInstalledStatus: MainViewModel.AppInstalledStatus) {
    AppMaterialTheme {
        PremiumLayout(
            appInstalledStatus = appInstalledStatus,
            watchFaceInstallButtonPressed = {},
            syncPremiumStatusButtonPressed = {},
            donateButtonPressed = {},
        )
    }
}