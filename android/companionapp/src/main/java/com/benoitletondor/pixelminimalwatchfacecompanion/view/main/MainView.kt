/*
 *   Copyright 2021 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.benoitletondor.pixelminimalwatchfacecompanion.view.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltNavGraphViewModel
import androidx.navigation.NavController
import androidx.navigation.activity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.benoitletondor.pixelminimalwatchfacecompanion.BuildConfig
import com.benoitletondor.pixelminimalwatchfacecompanion.R
import com.benoitletondor.pixelminimalwatchfacecompanion.helper.startSupportEmailActivity
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.AppMaterialTheme
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.components.AppTopBarMoreMenuItem
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.components.AppTopBarScaffold
import com.benoitletondor.pixelminimalwatchfacecompanion.view.donation.Donation
import com.benoitletondor.pixelminimalwatchfacecompanion.view.onboarding.OnboardingActivity
import com.benoitletondor.pixelminimalwatchfacecompanion.view.main.subviews.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainView()
        }
    }
}

@Composable
private fun MainView() {
    val navController = rememberNavController()

    AppMaterialTheme{
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                Main(navController, hiltNavGraphViewModel())
            }
            composable("donation") {
                Donation(navController, hiltNavGraphViewModel())
            }
            activity(R.id.navigation_onboarding) {
                activityClass = OnboardingActivity::class
            }
        }
    }
}

@Composable
private fun Main(navController: NavController, mainViewModel: MainViewModel) {
    val state: MainViewModel.State by mainViewModel.stateFlow.collectAsState(initial = mainViewModel.state)
    val context = LocalContext.current

    LaunchedEffect("nav") {
        launch {
            mainViewModel.navigationEventFlow.collect { navDestination ->
                when(navDestination) {
                    MainViewModel.NavigationDestination.Donate -> {
                        navController.navigate(R.id.navigation_donate)
                    }
                    MainViewModel.NavigationDestination.Onboarding -> {
                        navController.navigate(R.id.navigation_onboarding)
                    }
                    is MainViewModel.NavigationDestination.VoucherRedeem -> {
                        if ( !context.launchRedeemVoucherFlow(navDestination.voucherCode) ) {
                            AlertDialog.Builder(context)
                                .setTitle(R.string.iab_purchase_error_title)
                                .setMessage(R.string.iab_purchase_error_message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect("errors") {
        launch {
            mainViewModel.errorEventFlow.collect { errorType ->
                when(errorType) {
                    is MainViewModel.ErrorType.ErrorWhileSyncingWithWatch -> {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.error_syncing_title)
                            .setMessage(context.getString(R.string.error_syncing_message, errorType.error.message))
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                    MainViewModel.ErrorType.UnableToOpenPlayStoreOnWatch -> {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.playstore_not_opened_on_watch_title)
                            .setMessage(R.string.playstore_not_opened_on_watch_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                    is MainViewModel.ErrorType.UnableToPay -> {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.error_paying_title)
                            .setMessage(context.getString(R.string.error_paying_message, errorType.error.message))
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                }
            }
        }
    }

    LaunchedEffect("events") {
        launch {
            mainViewModel.eventFlow.collect { eventType ->
                when(eventType) {
                    MainViewModel.EventType.PLAY_STORE_OPENED_ON_WATCH -> {
                        Toast.makeText(context, R.string.playstore_opened_on_watch_message, Toast.LENGTH_LONG).show()
                    }
                    MainViewModel.EventType.SYNC_WITH_WATCH_SUCCEED -> {
                        Toast.makeText(context, R.string.sync_succeed_message, Toast.LENGTH_LONG).show()
                    }
                    MainViewModel.EventType.SHOW_VOUCHER_INPUT -> {
                        context.showRedeemVoucherUI { voucherInput ->
                            mainViewModel.onVoucherInput(voucherInput)
                        }
                    }
                }
            }
        }
    }

    AppTopBarScaffold(
        navController = navController,
        title = stringResource(R.string.app_name),
        actions = {
            AppTopBarMoreMenuItem {
                DropdownMenuItem(
                    onClick = { context.startSupportEmailActivity() },
                ) {
                    Text(stringResource(R.string.send_feedback_cta))
                }
                DropdownMenuItem(
                    enabled = false,
                    onClick = {},
                ) {
                    Text(stringResource(R.string.copyright, BuildConfig.VERSION_NAME))
                }
            }
        },
        content = {
            when(val currentState = state) {
                is MainViewModel.State.Error -> Error(state = currentState, viewModel = mainViewModel)
                MainViewModel.State.Loading -> Loading(viewModel = mainViewModel)
                is MainViewModel.State.NotPremium -> NotPremium(state = currentState, viewModel = mainViewModel)
                is MainViewModel.State.Premium -> Premium(state = currentState, viewModel = mainViewModel)
                is MainViewModel.State.Syncing -> Syncing(state = currentState, viewModel = mainViewModel)
            }
        }
    )
}

private fun Context.launchRedeemVoucherFlow(voucher: String): Boolean {
    return try {
        val url = "https://play.google.com/redeem?code=" + URLEncoder.encode(voucher, "UTF-8")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (e: Exception) {
        false
    }
}

private fun Context.showRedeemVoucherUI(onVoucherInput: (String) -> Unit) {
    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_redeem_voucher, null)
    val voucherEditText: EditText = dialogView.findViewById(R.id.voucher)

    val builder = android.app.AlertDialog.Builder(this)
        .setTitle(R.string.voucher_redeem_dialog_title)
        .setMessage(R.string.voucher_redeem_dialog_message)
        .setView(dialogView)
        .setPositiveButton(R.string.voucher_redeem_dialog_cta) { dialog, _ ->
            dialog.dismiss()

            val voucher = voucherEditText.text.toString()
            if (voucher.trim { it <= ' ' }.isEmpty()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle(R.string.voucher_redeem_error_dialog_title)
                    .setMessage(R.string.voucher_redeem_error_code_invalid_dialog_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()

                return@setPositiveButton
            }

            onVoucherInput(voucher)
        }
        .setNegativeButton(android.R.string.cancel, null)

    val dialog = builder.show()

    // Directly show keyboard when the dialog pops
    voucherEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        // Check if the device doesn't have a physical keyboard
        if (hasFocus && resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }
}