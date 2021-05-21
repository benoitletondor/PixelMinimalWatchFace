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
package com.benoitletondor.pixelminimalwatchfacecompanion.view.donation

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.SkuDetails
import com.benoitletondor.pixelminimalwatchfacecompanion.billing.Billing
import com.benoitletondor.pixelminimalwatchfacecompanion.helper.MutableLiveFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DonationViewModel @Inject constructor(
    private val billing: Billing,
) : ViewModel() {
    val errorPayingEventMutableFlow = MutableLiveFlow<Throwable>()
    val errorPayingEventFlow: Flow<Throwable> = errorPayingEventMutableFlow

    val donationSuccessEventMutableFlow = MutableLiveFlow<SkuDetails>()
    val donationSuccessEventFlow: Flow<SkuDetails> = donationSuccessEventMutableFlow

    val stateMutableFlow = MutableStateFlow<State>(State.Loading)
    val stateFlow: Flow<State> = stateMutableFlow

    init {
        loadSKUs()
    }

    private fun loadSKUs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                stateMutableFlow.value = State.Loading

                try {
                    stateMutableFlow.value = State.Load(billing.getDonationsSKUs())
                } catch (error: Throwable) {
                    Log.e("DonationViewModel", "Error while loading SKUs", error)
                    stateMutableFlow.value = State.ErrorLoading(error)
                }
            }
        }
    }

    fun onRetryLoadSKUsButtonClicked() {
        loadSKUs()
    }

    fun onDonateButtonClicked(sku: SkuDetails, activity: Activity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val purchaseStatus = billing.launchDonationPurchaseFlow(activity, sku)
                    if( purchaseStatus ) {
                        donationSuccessEventMutableFlow.emit(sku)
                    }
                } catch (error: Throwable) {
                    Log.e("DonationViewModel", "Error while donation for SKU: ${sku.sku}", error)
                    errorPayingEventMutableFlow.emit(error)
                }
            }
        }
    }

    sealed class State {
        object Loading : State()
        class ErrorLoading(val error: Throwable) : State()
        class Load(val SKUs: List<SkuDetails>) : State()
    }
}