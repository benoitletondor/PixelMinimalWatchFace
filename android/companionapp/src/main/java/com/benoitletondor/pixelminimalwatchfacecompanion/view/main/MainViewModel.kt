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

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benoitletondor.pixelminimalwatchfacecompanion.billing.Billing
import com.benoitletondor.pixelminimalwatchfacecompanion.billing.PremiumCheckStatus
import com.benoitletondor.pixelminimalwatchfacecompanion.billing.PremiumPurchaseFlowResult
import com.benoitletondor.pixelminimalwatchfacecompanion.config.Config
import com.benoitletondor.pixelminimalwatchfacecompanion.config.getVouchers
import com.benoitletondor.pixelminimalwatchfacecompanion.helper.MutableLiveFlow
import com.benoitletondor.pixelminimalwatchfacecompanion.storage.Storage
import com.benoitletondor.pixelminimalwatchfacecompanion.sync.Sync
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val billing: Billing,
    private val sync: Sync,
    private val config: Config,
    private val storage: Storage
) : ViewModel(), CapabilityClient.OnCapabilityChangedListener {
    private val navigationEventMutableFlow = MutableLiveFlow<NavigationDestination>()
    val navigationEventFlow: Flow<NavigationDestination> = navigationEventMutableFlow

    private val errorEventMutableFlow = MutableLiveFlow<ErrorType>()
    val errorEventFlow: Flow<ErrorType> = errorEventMutableFlow

    private val eventMutableFlow = MutableLiveFlow<EventType>()
    val eventFlow: Flow<EventType> = eventMutableFlow

    private val lastSyncedPremiumStatusStateFlow = MutableStateFlow<Boolean?>(null)
    private val isSyncingStateFlow = MutableStateFlow(false)
    private val userIsBuyingPremiumStateFlow = MutableStateFlow(false)
    private val appInstalledStatusStateFlow = MutableStateFlow<AppInstalledStatus>(AppInstalledStatus.Unknown)

    private val stateStateFlow = combine(
        billing.userPremiumEventStream,
        userIsBuyingPremiumStateFlow,
        appInstalledStatusStateFlow,
        isSyncingStateFlow,
        lastSyncedPremiumStatusStateFlow,
        MainViewModel::computeState,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)
    val stateFlow: Flow<State> = stateStateFlow
    val state: State get() = stateStateFlow.value

    init {
        if( !storage.isOnboardingFinished() ) {
            viewModelScope.launch {
                navigationEventMutableFlow.emit(NavigationDestination.Onboarding)
            }
        }

        viewModelScope.launch {
            billing.userPremiumEventStream
                .collect { premiumStatus ->
                    if( (premiumStatus == PremiumCheckStatus.Premium && lastSyncedPremiumStatusStateFlow.value == false) ||
                        (premiumStatus == PremiumCheckStatus.NotPremium && lastSyncedPremiumStatusStateFlow.value == true) ||
                        (premiumStatus == PremiumCheckStatus.Premium || premiumStatus == PremiumCheckStatus.NotPremium) && lastSyncedPremiumStatusStateFlow.value == null ) {
                        syncState(premiumStatus == PremiumCheckStatus.Premium)
                    }
                }
        }

        syncAppInstalledStatus()
        sync.subscribeToCapabilityChanges(this)
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        syncAppInstalledStatus()
    }

    private fun syncState(userPremium: Boolean) {
        viewModelScope.launch {
            try {
                isSyncingStateFlow.value = true

                withContext(Dispatchers.IO) {
                    sync.sendPremiumStatus(userPremium)
                }

                lastSyncedPremiumStatusStateFlow.value = userPremium

                if( userPremium ) {
                    eventMutableFlow.emit(EventType.SYNC_WITH_WATCH_SUCCEED)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }

                if (state is State.Premium) {
                    errorEventMutableFlow.emit(ErrorType.ErrorWhileSyncingWithWatch(t))
                }
            } finally {
                isSyncingStateFlow.value = false
            }
        }
    }

    override fun onCleared() {
        sync.unsubscribeToCapabilityChanges(this)

        super.onCleared()
    }

    fun triggerSync() {
        syncState(billing.isUserPremium())
    }

    fun retryPremiumStatusCheck() {
        billing.updatePremiumStatusIfNeeded()
    }

    fun launchPremiumBuyFlow(host: Activity) {
        viewModelScope.launch {
            try {
                userIsBuyingPremiumStateFlow.value = true

                val result = withContext(Dispatchers.IO) {
                    billing.launchPremiumPurchaseFlow(host)
                }

                // Success result will be handled automatically as notification to userPremiumEventObserver

                if( result is PremiumPurchaseFlowResult.Error ){
                    errorEventMutableFlow.emit(ErrorType.UnableToPay(Exception(result.reason)))
                }
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }

                errorEventMutableFlow.emit(ErrorType.UnableToPay(t))
            } finally {
                userIsBuyingPremiumStateFlow.value = false
            }
        }
    }

    fun onVoucherInput(voucher: String) {
        val vouchers = config.getVouchers()
        if( vouchers.contains(voucher) ) {
            storage.setUserPremium(true)
            syncState(true)

            return
        }

        viewModelScope.launch {
            navigationEventMutableFlow.emit(NavigationDestination.VoucherRedeem(voucher))
        }
    }

    fun onInstallWatchFaceButtonPressed() {
        viewModelScope.launch {
            try {
                if (sync.openPlayStoreOnWatch()) {
                    eventMutableFlow.emit(EventType.PLAY_STORE_OPENED_ON_WATCH)
                } else {
                    errorEventMutableFlow.emit(ErrorType.UnableToOpenPlayStoreOnWatch)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }

                Log.e("MainViewModel", "Error opening PlayStore", t)
                errorEventMutableFlow.emit(ErrorType.UnableToOpenPlayStoreOnWatch)
            }
        }
    }

    fun onDonateButtonPressed() {
        viewModelScope.launch {
            navigationEventMutableFlow.emit(NavigationDestination.Donate)
        }
    }

    private fun syncAppInstalledStatus() {
        viewModelScope.launch {
            appInstalledStatusStateFlow.value = AppInstalledStatus.Verifying
            appInstalledStatusStateFlow.value = AppInstalledStatus.Result(sync.getWearableStatus())
        }
    }

    sealed class State {
        object Loading : State()
        class NotPremium(val appInstalledStatus: AppInstalledStatus) : State()
        class Syncing(val isUserPremium: Boolean) : State()
        class Premium(val appInstalledStatus: AppInstalledStatus) : State()
        class Error(val error: Throwable) : State()
    }

    sealed class NavigationDestination {
        object Onboarding : NavigationDestination()
        class VoucherRedeem(val voucherCode: String) : NavigationDestination()
        class PlayStore()
        object Donate : NavigationDestination()
    }

    sealed class ErrorType {
        class ErrorWhileSyncingWithWatch(val error: Throwable) : ErrorType()
        class UnableToPay(val error: Throwable) : ErrorType()
        object UnableToOpenPlayStoreOnWatch : ErrorType()
    }

    enum class EventType {
        PLAY_STORE_OPENED_ON_WATCH,
        SYNC_WITH_WATCH_SUCCEED,
    }

    sealed class AppInstalledStatus {
        object Unknown : AppInstalledStatus()
        object Verifying : AppInstalledStatus()
        class Result(val wearableStatus: Sync.WearableStatus) : AppInstalledStatus()
    }

    companion object {
        private fun computeState(
            premiumStatus: PremiumCheckStatus,
            userIsBuyingPremium: Boolean,
            appInstalledStatus: AppInstalledStatus,
            isSyncing: Boolean,
            lastSyncedPremiumStatus: Boolean?,
        ) : State {
            if (userIsBuyingPremium) {
                return State.Loading
            }

            if (isSyncing) {
                return State.Syncing(premiumStatus == PremiumCheckStatus.Premium)
            }

            if (lastSyncedPremiumStatus != null) {
                return if (premiumStatus == PremiumCheckStatus.Premium) {
                    State.Premium(appInstalledStatus)
                } else {
                    State.NotPremium(appInstalledStatus)
                }
            }

            return when(premiumStatus) {
                PremiumCheckStatus.Checking -> State.Loading
                is PremiumCheckStatus.Error ->  State.Error(premiumStatus.error)
                PremiumCheckStatus.Initializing -> State.Loading
                PremiumCheckStatus.NotPremium -> State.NotPremium(appInstalledStatus)
                PremiumCheckStatus.Premium -> State.Premium(appInstalledStatus)
            }
        }
    }
}