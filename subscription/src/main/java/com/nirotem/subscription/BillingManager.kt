package com.nirotem.subscription

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import java.util.concurrent.TimeUnit

sealed class PurchaseStatus {
    object NotPurchased : PurchaseStatus()
    data class InTrial(val daysLeft: Int) : PurchaseStatus()
    object Purchased : PurchaseStatus()
}

class BillingManager(
    private val specificAppContext: Context,
    private val onPurchaseUpdated: (PurchaseStatus) -> Unit
) {

    private val trialDays = 7L // כמה ימים יש לניסיון חינם

    // מאזין לרכישות חדשות
    // בפועל זה מאזין קבוע:
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchaseList(purchases)
            onPurchaseUpdated(PurchaseStatus.Purchased)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onPurchaseUpdated(PurchaseStatus.NotPurchased)
        }
    }

    // האובייקט שמדבר עם Google Play
    private val billingClient: BillingClient = BillingClient.newBuilder(specificAppContext)
        .enablePendingPurchases()
        .setListener(purchasesUpdatedListener)
        .build()

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                } else {
                    onPurchaseUpdated(PurchaseStatus.NotPurchased)
                }
            }

            override fun onBillingServiceDisconnected() {
                // אפשרות לחיבור מחדש
            }
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) {
                onPurchaseUpdated(PurchaseStatus.NotPurchased)
                return@queryPurchasesAsync
            }

            handlePurchaseList(purchases)
        }
    }

    private fun handlePurchaseList(purchases: List<Purchase>) {
        val purchase = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        if (purchase != null) {
            val daysLeft = calculateTrialDaysLeft(purchase)
            if (daysLeft > 0) {
                onPurchaseUpdated(PurchaseStatus.InTrial(daysLeft))
            } else {
                onPurchaseUpdated(PurchaseStatus.Purchased)
            }
        } else {
            onPurchaseUpdated(PurchaseStatus.NotPurchased)
        }
    }

    private fun calculateTrialDaysLeft(purchase: Purchase): Int {
        val now = System.currentTimeMillis()
        val elapsed = now - purchase.purchaseTime
        val trialMillis = TimeUnit.DAYS.toMillis(trialDays)
        val remainingMillis = trialMillis - elapsed
        return if (remainingMillis > 0)
            (remainingMillis / TimeUnit.DAYS.toMillis(1)).toInt()
        else
            0
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]

                val offerToken = productDetails.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.offerToken ?: return@queryProductDetailsAsync

                val offer = productDetails.subscriptionOfferDetails?.firstOrNull()
                val pricingPhase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()

                val priceFormatted = pricingPhase?.formattedPrice  // למשל: "$2.00"

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    ).build()

                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    fun queryPrices(productIds: List<String>, onResult: (Map<String, ProductDetails>) -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            ).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                onResult(map)
            } else {
                onResult(emptyMap())
            }
        }
    }

}
