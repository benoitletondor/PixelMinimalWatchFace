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
package com.benoitletondor.pixelminimalwatchfacecompanion.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.benoitletondor.pixelminimalwatchfacecompanion.storage.Storage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * SKU premium
 */
private const val SKU_PREMIUM = "premium"

/**
 * SKUs donation
 */
const val SKU_DONATION_TIER_1 = "donation_tier_1"
const val SKU_DONATION_TIER_2 = "donation_tier_2"
const val SKU_DONATION_TIER_3 = "donation_tier_3"
const val SKU_DONATION_TIER_4 = "donation_tier_4"
const val SKU_DONATION_TIER_5 = "donation_tier_5"

class BillingImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val storage: Storage
) : Billing, PurchasesUpdatedListener, BillingClientStateListener, PurchaseHistoryResponseListener, AcknowledgePurchaseResponseListener {

    private val appContext = context.applicationContext
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    /**
     * iab check status
     */
    private var iabStatus: PremiumCheckStatus = PremiumCheckStatus.Initializing

    private var premiumFlowContinuation: Continuation<PremiumPurchaseFlowResult>? = null
    private var donationFlowContinuation: Continuation<Boolean>? = null

    override val userPremiumEventStream: Flow<PremiumCheckStatus>
        get() = userPremiumEventSteamInternal

    private val userPremiumEventSteamInternal = MutableStateFlow<PremiumCheckStatus>(PremiumCheckStatus.Initializing)

    init {
        startBillingClient()
    }

    private fun startBillingClient() {
        try {
            setIabStatusAndNotify(PremiumCheckStatus.Initializing)

            billingClient.startConnection(this)
        } catch (e: Exception) {
            Log.e("BillingImpl", "Error while checking iab status", e)
            setIabStatusAndNotify(PremiumCheckStatus.Error(e))
        }
    }

    /**
     * Set the new iab status and notify the app by an event
     *
     * @param status the new status
     */
    private fun setIabStatusAndNotify(status: PremiumCheckStatus) {
        iabStatus = status

        if ( status == PremiumCheckStatus.Premium ) {
            storage.setUserPremium(true)
        }

        // Case if a voucher has been redeemed
        if( status == PremiumCheckStatus.NotPremium && storage.isUserPremium() ) {
            userPremiumEventSteamInternal.value = PremiumCheckStatus.Premium
            return
        }

        userPremiumEventSteamInternal.value = status
    }

    /**
     * Is the user a premium user
     *
     * @return true if the user if premium, false otherwise
     */
    override fun isUserPremium(): Boolean
    {
        return storage.isUserPremium() || iabStatus == PremiumCheckStatus.Premium
    }

    /**
     * Update the current IAP status if already checked
     */
    override fun updatePremiumStatusIfNeeded() {
        Log.d("BillingImpl", "updateIAPStatusIfNeeded: $iabStatus")

        if ( iabStatus == PremiumCheckStatus.NotPremium ) {
            setIabStatusAndNotify(PremiumCheckStatus.Checking)
            billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, this)
        } else if ( iabStatus is PremiumCheckStatus.Error) {
            startBillingClient()
        }
    }

    /**
     * Launch the premium purchase flow
     *
     * @param activity activity that started this purchase
     */
    override suspend fun launchPremiumPurchaseFlow(activity: Activity): PremiumPurchaseFlowResult {
        if ( iabStatus != PremiumCheckStatus.NotPremium ) {
            return when (iabStatus) {
                is PremiumCheckStatus.Error -> PremiumPurchaseFlowResult.Error("Unable to connect to your Google account. Please restart the app and try again")
                PremiumCheckStatus.Premium -> PremiumPurchaseFlowResult.Error("You already bought Premium with that Google account. Restart the app if you don't have access to premium features.")
                else -> PremiumPurchaseFlowResult.Error("Runtime error: $iabStatus")
            }
        }

        val skuList = ArrayList<String>(1)
        skuList.add(SKU_PREMIUM)

        val (billingResult, skuDetailsList) = billingClient.querySkuDetails(
            SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
                .build()
        )

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                setIabStatusAndNotify(PremiumCheckStatus.Premium)
                return PremiumPurchaseFlowResult.Success
            }

            return PremiumPurchaseFlowResult.Error("Unable to connect to reach PlayStore (response code: " + billingResult.responseCode + "). Please restart the app and try again")
        }

        if (skuDetailsList == null || skuDetailsList.isEmpty()) {
            return PremiumPurchaseFlowResult.Error("Unable to fetch content from PlayStore (response code: skuDetailsList is empty). Please restart the app and try again")
        }

        return suspendCoroutine { continuation ->
            premiumFlowContinuation = continuation

            billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetailsList[0])
                .build()
            )
        }
    }

    override suspend fun getDonationsSKUs(): List<SkuDetails> {
        if( iabStatus is PremiumCheckStatus.Initializing || iabStatus is PremiumCheckStatus.Error ) {
            throw IllegalStateException("IAB is not setup")
        }

        val skuList = listOf(
            SKU_DONATION_TIER_1,
            SKU_DONATION_TIER_2,
            SKU_DONATION_TIER_3,
            SKU_DONATION_TIER_4,
            SKU_DONATION_TIER_5,
        )
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        val (billingResult, skuDetailsList) = billingClient.querySkuDetails(params.build())

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            throw IllegalStateException("Unable to connect to reach PlayStore (response code: " + billingResult.responseCode + "). Please restart the app and try again")
        }

        if( skuDetailsList == null ) {
            throw IllegalStateException("Unable to get details from PlayStore. Please restart the app and try again")
        }

        return skuDetailsList
    }

    override suspend fun launchDonationPurchaseFlow(activity: Activity, sku: SkuDetails): Boolean {
        return suspendCancellableCoroutine { continuation ->
            donationFlowContinuation = continuation

            billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setSkuDetails(sku)
                .build()
            )
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d("BillingImpl", "iab setup finished.")

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            // Oh noes, there was a problem.
            setIabStatusAndNotify(PremiumCheckStatus.Error(Exception("Error while setting-up iab: " + billingResult.responseCode)))
            Log.e("BillingImpl","Error while setting-up iab: " + billingResult.responseCode)
            return
        }

        setIabStatusAndNotify(PremiumCheckStatus.Checking)

        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, this)
    }

    override fun onBillingServiceDisconnected() {
        Log.d("BillingImpl", "onBillingServiceDisconnected")

        premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("Lost connection with Google Play")))
        setIabStatusAndNotify(PremiumCheckStatus.Error(IOException("Lost connection with Google Play")))
    }

    override fun onPurchaseHistoryResponse(billingResult: BillingResult, purchaseHistoryRecordList: List<PurchaseHistoryRecord>?) {
        Log.d("BillingImpl", "iab query inventory finished.")

        // Is it a failure?
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e("BillingImpl", "Error while querying iab inventory: " + billingResult.responseCode)
            setIabStatusAndNotify(PremiumCheckStatus.Error(Exception("Error while querying iab inventory: " + billingResult.responseCode)))
            return
        }

        var premium = false
        if (purchaseHistoryRecordList != null) {
            for (purchase in purchaseHistoryRecordList) {
                if (SKU_PREMIUM == purchase.sku) {
                    premium = true
                }
            }
        }

        Log.d("BillingImpl", "iab query inventory was successful: $premium")

        setIabStatusAndNotify(if (premium) PremiumCheckStatus.Premium else PremiumCheckStatus.NotPremium)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if( premiumFlowContinuation != null ) {
            Log.d("BillingImpl", "Purchase finished: " + billingResult.responseCode)

            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e("BillingImpl", "Error while purchasing premium: " + billingResult.responseCode)
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Cancelled))
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        setIabStatusAndNotify(PremiumCheckStatus.Premium)
                        premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Success))
                        return
                    }
                    else -> premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("An error occurred (status code: " + billingResult.responseCode + ")")))
                }

                premiumFlowContinuation = null
                return
            }


            if ( purchases.isNullOrEmpty() ) {
                premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("No purchased item found")))
                premiumFlowContinuation = null
                return
            }

            Log.d("BillingImpl", "Purchase successful.")

            for (purchase in purchases) {
                if (SKU_PREMIUM == purchase.sku) {
                    billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(), this)
                    return
                }
            }

            premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("No purchased item found")))
            premiumFlowContinuation = null
        } else if( donationFlowContinuation != null ) {
            Log.d("BillingImpl", "Purchase donation finished: " + billingResult.responseCode)

            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e("BillingImpl", "Error while purchasing donation: " + billingResult.responseCode)
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> donationFlowContinuation?.resumeWith(Result.success(false))
                    else -> donationFlowContinuation?.resumeWith(Result.failure(IllegalStateException("An error occurred (status code: " + billingResult.responseCode + ")")))
                }

                donationFlowContinuation = null
                return
            }

            if ( purchases.isNullOrEmpty() ) {
                donationFlowContinuation?.resumeWith(Result.failure(IllegalStateException("An error occurred: No purchases returned")))
                donationFlowContinuation = null
                return
            }

            var hasPurchaseToAck = false
            for (purchase in purchases) {
                if( !purchase.isAcknowledged ) {
                    hasPurchaseToAck = true
                    Log.d("BillingImpl", "Consume donation.")

                    val consumeParams =
                        ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()

                    billingClient.consumeAsync(consumeParams) { consumeBillingResult, outToken ->
                        Log.d("BillingImpl", "Consume donation finished with result: ${consumeBillingResult.debugMessage}")
                        if (consumeBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            donationFlowContinuation?.resumeWith(Result.success(true))
                        } else {
                            donationFlowContinuation?.resumeWith(
                                Result.failure(
                                    IllegalStateException("Error when consuming purchase with Google (${billingResult.responseCode}, ${billingResult.debugMessage}). Please try again")
                                )
                            )
                        }

                        donationFlowContinuation = null
                    }
                }
            }

            if( !hasPurchaseToAck ) {
                donationFlowContinuation?.resumeWith(Result.failure(IllegalStateException("Unable to find your donation, please try again or contact Google.")))
                donationFlowContinuation = null
            }
        } else {
            Log.w("BillingImpl", "Got onPurchasesUpdated result without a listener with status: ${billingResult.responseCode}. Ignoring")
        }
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        Log.d("BillingImpl", "Acknowledge premium successful.")

        if( billingResult.responseCode != BillingClient.BillingResponseCode.OK ) {
            premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Error("Error when acknowledging purchase with Google (${billingResult.responseCode}, ${billingResult.debugMessage}). Please try again")))
            premiumFlowContinuation = null
            return
        }

        setIabStatusAndNotify(PremiumCheckStatus.Premium)
        premiumFlowContinuation?.resumeWith(Result.success(PremiumPurchaseFlowResult.Success))
        premiumFlowContinuation = null
    }
}