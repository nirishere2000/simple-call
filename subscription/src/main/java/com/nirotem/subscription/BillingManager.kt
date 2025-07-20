package com.nirotem.subscription

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.android.billingclient.api.*
import java.time.Period
import java.util.concurrent.TimeUnit

sealed class PurchaseStatus {
    data class PurchasedBasic(val purchase: Purchase) : PurchaseStatus()
    data class PurchasedPremium(val purchase: Purchase) : PurchaseStatus()
    data class InTrial(val daysLeft: Int, val isPremium: Boolean) : PurchaseStatus()
    object NotPurchased : PurchaseStatus()
}

class BillingManager(
    private val specificAppContext: Context,
    private val onPurchaseUpdated: (PurchaseStatus) -> Unit
) {

    companion object {
        const val PREMIUM_MONTHLY_PLAN_ID = "easy-call-and-answer-premium-monthly"
        const val BASIC_MONTHLY_PLAN_ID = "easy-call-and-answer-basic-monthly"
       // const val BASIC_ANNUAL_PLAN_ID = "easy-call-and-answer-basic-annual"
       // const val PREMIUM_ANNUAL_PLAN_ID = "easy-call-and-answer-premium-annual"
    }

    private val defaultTrialDays = 7L // ברירת מחדל אם אין מידע מהקונסול
    private var pendingPurchaseCallback: ((PurchaseStatus) -> Unit)? = null
    private var trialDaysFromServer: Long? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(params) { ackResult ->
                            if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                dispatchPurchase(purchase)
                            } else {
                                Log.e("BillingManager", "Acknowledge failed: ${ackResult.debugMessage}")
                            }
                        }
                    } else {
                        dispatchPurchase(purchase)
                    }
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onPurchaseUpdated(PurchaseStatus.NotPurchased)
        }
    }

    private fun dispatchPurchase(purchase: Purchase) {
        val daysLeft = calculateTrialDaysLeft(purchase)

        val isBasic = purchase.products.any { it == BASIC_MONTHLY_PLAN_ID } // || it == BASIC_ANNUAL_PLAN_ID }
        val isPremium = purchase.products.any { it == PREMIUM_MONTHLY_PLAN_ID } // || it == PREMIUM_ANNUAL_PLAN_ID }

        val result = when {
            daysLeft > 0 -> PurchaseStatus.InTrial(daysLeft, isPremium)
            isPremium -> PurchaseStatus.PurchasedPremium(purchase)
            isBasic -> PurchaseStatus.PurchasedBasic(purchase)
            else -> PurchaseStatus.NotPurchased
        }

        // קורא לפונקציית התוצאה אם מישהו מחכה לה
        pendingPurchaseCallback?.invoke(result)
        pendingPurchaseCallback = null

        // וגם שולח לקוד הקבוע שלך
        onPurchaseUpdated(result)
    }

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

            override fun onBillingServiceDisconnected() {}
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
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("BillingUpgrade", "Purchase acknowledged.")
                        handleAcknowledgedPurchase(purchase)
                    } else {
                        Log.e("BillingUpgrade", "Failed to acknowledge: ${result.debugMessage}")
                        onPurchaseUpdated(PurchaseStatus.NotPurchased)
                    }
                }
            } else {
                handleAcknowledgedPurchase(purchase)
            }
        } else {
            onPurchaseUpdated(PurchaseStatus.NotPurchased)
        }
    }

    private fun handleAcknowledgedPurchase(purchase: Purchase) {
        val daysLeft = calculateTrialDaysLeft(purchase)

        val isBasic = purchase.products.any { it == BASIC_MONTHLY_PLAN_ID } // || it == BASIC_ANNUAL_PLAN_ID }
        val isPremium = purchase.products.any { it == PREMIUM_MONTHLY_PLAN_ID } // || it == PREMIUM_ANNUAL_PLAN_ID }

        if (daysLeft > 0) {
            val isPremium = purchase.products.any { it == PREMIUM_MONTHLY_PLAN_ID } // || it == PREMIUM_ANNUAL_PLAN_ID }
            onPurchaseUpdated(PurchaseStatus.InTrial(daysLeft, isPremium))
        } else {
            when {
                isPremium -> onPurchaseUpdated(PurchaseStatus.PurchasedPremium(purchase))
                isBasic -> onPurchaseUpdated(PurchaseStatus.PurchasedBasic(purchase))
                else -> {
                    Log.w("BillingManager", "רכישה לא מזוהה: ${purchase.products}")
                    onPurchaseUpdated(PurchaseStatus.NotPurchased)
                }
            }
        }
    }

    private fun calculateTrialDaysLeft(purchase: Purchase): Int {
        val now = System.currentTimeMillis()
        val elapsed = now - purchase.purchaseTime

        val trialMillis = TimeUnit.DAYS.toMillis(trialDaysFromServer ?: defaultTrialDays)
        val remainingMillis = trialMillis - elapsed
        return if (remainingMillis > 0)
            (remainingMillis / TimeUnit.DAYS.toMillis(1)).toInt()
        else
            0
    }

    fun launchPurchaseFlow(
        activity: Activity,
        newProductId: String,
        onResult: (PurchaseStatus) -> Unit
    ) {
        pendingPurchaseCallback = onResult // נשמר לרגע סיום הרכישה דרך listener

        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchases ->
            val existingPurchase = purchases.firstOrNull { purchase ->
                listOf(BASIC_MONTHLY_PLAN_ID, PREMIUM_MONTHLY_PLAN_ID).any {
                    purchase.products.contains(it)
                }
            }

            val productQuery = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(newProductId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                ).build()

            billingClient.queryProductDetailsAsync(productQuery) { productResult, detailsList ->
                if (productResult.responseCode != BillingClient.BillingResponseCode.OK || detailsList.isEmpty()) {
                    Log.e("BillingManager", "Failed to load product details")
                    // 🛑 עדכון חשוב – מחזיר תשובה על כישלון לפני שפותחים Google Play
                    onResult(PurchaseStatus.NotPurchased)
                    return@queryProductDetailsAsync
                }

                val productDetails = detailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken == null) {
                    Log.e("BillingManager", "Offer token not found")
                    onResult(PurchaseStatus.NotPurchased)
                    return@queryProductDetailsAsync
                }

                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()

                val builder = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))

                if (existingPurchase != null) {
                    val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(existingPurchase.purchaseToken)
                        .setReplaceProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
                        .build()

                    builder.setSubscriptionUpdateParams(updateParams)
                }

                val launchResult = billingClient.launchBillingFlow(activity, builder.build())

                // ✅ במקרה של כישלון מיידי ב־launchBillingFlow עצמו (למשל: אין חיבור, וכו')
                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e("BillingManager", "launchBillingFlow failed: ${launchResult.debugMessage}")
                    onResult(PurchaseStatus.NotPurchased)
                }

                // ✅ במקרה של הצלחה – התשובה תגיע דרך purchasesUpdatedListener
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun extractTrialDaysFromProductDetails(productDetails: ProductDetails) {
        val offer = productDetails.subscriptionOfferDetails?.firstOrNull()
        val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()

        if (phase != null && phase.priceAmountMicros == 0L) {
            val periodString = phase.billingPeriod // לדוגמה: "P7D"
            try {
                val period = Period.parse(periodString)
                trialDaysFromServer = period.days.toLong()
                Log.d("Billing", "Trial days from server: $trialDaysFromServer")
            } catch (e: Exception) {
                trialDaysFromServer = defaultTrialDays
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

    fun featureOnlyAvailableOnPremiumAlert(context: Context, activity: Activity, onResult: (PurchaseStatus) -> Unit) {
        val dialog = Dialog(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_premium_upgrade, null)

        val monthlyButton = dialogView.findViewById<Button>(R.id.btn_premium_monthly)
        //val annualButton = dialogView.findViewById<Button>(R.id.btn_premium_annual)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        val errorTextView = dialogView.findViewById<TextView>(R.id.results_error)

        fun handleUpgrade(productId: String) {
            errorTextView.text = context.getString(R.string.dialog_promo_processing)
            errorTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))

            upgradeToPremium(
                activity = activity,
                premiumProductId = productId,
                basicProductIds = listOf(BASIC_MONTHLY_PLAN_ID), //, BASIC_ANNUAL_PLAN_ID),
                onLaunchResult = { launchResult ->
                    if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        errorTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                        errorTextView.text = context.getString(R.string.billing_error_generic) +
                                " (code ${launchResult.responseCode})"
                    }
                    // אחרת — פשוט מחכים ל־PurchasesUpdatedListener
                },
                onFinalPurchaseResult = { status ->
                    when (status) {
                        is PurchaseStatus.PurchasedPremium,
                        is PurchaseStatus.PurchasedBasic,
                        is PurchaseStatus.InTrial -> {
                            dialog.dismiss()
                            onResult(status) // ⬅️ תחזיר לפרגמנט שקרא
                        }

                        is PurchaseStatus.NotPurchased -> {
                            errorTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                            errorTextView.text = context.getString(R.string.billing_error_generic)
                            onResult(status) // ⬅️ גם במקרה של כישלון
                        }
                    }
                }
            )
        }

        monthlyButton.setOnClickListener {
            handleUpgrade(PREMIUM_MONTHLY_PLAN_ID)
        }

/*
        annualButton.setOnClickListener {
            handleUpgrade(PREMIUM_ANNUAL_PLAN_ID)
        }
*/

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#565859")))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }


    fun upgradeToPremium(
        activity: Activity,
        premiumProductId: String,
        basicProductIds: List<String>,
        onLaunchResult: (BillingResult) -> Unit,               // ← רק לפתיחת Google Play
        onFinalPurchaseResult: (PurchaseStatus) -> Unit        // ← זה מה שתחכה לו בפועל
    ) {
        pendingPurchaseCallback = onFinalPurchaseResult

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases.isEmpty()) {
                onLaunchResult(billingResult)
                return@queryPurchasesAsync
            }

            val currentPurchase = purchases.firstOrNull {
                basicProductIds.any { id -> it.products.contains(id) }
            }

            if (currentPurchase == null) {
                onLaunchResult(billingResult)
                return@queryPurchasesAsync
            }

            val oldToken = currentPurchase.purchaseToken

            // שלב: לשאול את פרטי המוצר החדש
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(premiumProductId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                ).build()

            billingClient.queryProductDetailsAsync(queryParams) { result, productDetailsList ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isEmpty()) {
                    onLaunchResult(result)
                    return@queryProductDetailsAsync
                }

                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken == null) {
                    onLaunchResult(result)
                    return@queryProductDetailsAsync
                }

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    )
                    .setSubscriptionUpdateParams(
                        BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                            .setOldPurchaseToken(oldToken)
                            .setReplaceProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION)
                            .build()
                    )
                    .build()

                // ⬇️ רק פותח את Google Play – לא מחזיר תשובה על רכישה
                val billingResult = billingClient.launchBillingFlow(activity, flowParams)
                onLaunchResult(billingResult)
            }
        }
    }


}
