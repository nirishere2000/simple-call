package com.nirotem.subscription

import android.animation.AnimatorInflater
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class UpgradeDialogFragment(
    private val appSpecificFeatures: List<FeatureRow>,
    private val onResult: (SubscriptionResult) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.view_upgrade_overlay, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.96).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Make the dialog non-cancelable (optional)
        dialog?.setCancelable(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
       // val btnClose = view.findViewById<View>(R.id.upgrade_dialog_close_button)
        val btnStandard = view.findViewById<Button>(R.id.btnBasic)
        val btnPremium = view.findViewById<Button>(R.id.btnPremium)
        val basicPriceMonthly = view.findViewById<TextView>(R.id.basicPriceMonthly)
        val basicPriceYearly = view.findViewById<TextView>(R.id.basicPriceYearly)
        val premiumPriceMonthly = view.findViewById<TextView>(R.id.premiumPriceMonthly)
        val premiumPriceYearly = view.findViewById<TextView>(R.id.premiumPriceYearly)
        val promoButton = view.findViewById<TextView>(R.id.txtPromoCode)
/*        val premiumOriginalPrice = view.findViewById<TextView>(R.id.premiumOriginalPrice)
        premiumOriginalPrice.apply {
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
        val basicOriginalPrice = view.findViewById<TextView>(R.id.basicOriginalPrice)
        basicOriginalPrice.apply {
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }*/

        val billingManager = BillingManager(requireContext()) { purchaseStatus ->
            when (purchaseStatus) {
                is PurchaseStatus.Purchased -> {
                    onResult(SubscriptionResult.SubscribedPremium) // או Basic לפי המוצר שברכישה
                    dismissAllowingStateLoss()
                }
                is PurchaseStatus.NotPurchased -> {
                    onResult(SubscriptionResult.Canceled)
                    dismissAllowingStateLoss()
                }
                is PurchaseStatus.InTrial -> {
                    onResult(SubscriptionResult.SubscribedBasic)
                    dismissAllowingStateLoss()
                }
            }
        }


        val basicFeaturesContainer = view.findViewById<LinearLayout>(R.id.basicFeaturesContainer)
        val premiumFeaturesContainer = view.findViewById<LinearLayout>(R.id.premiumFeaturesContainer)

        billingManager.queryPrices(
            listOf("premium_subscription_id", "basic_subscription_id")
        ) { productDetailsMap ->
            // ----- PREMIUM -----
            val premium = productDetailsMap["premium_subscription_id"]
            premium?.subscriptionOfferDetails?.let { offers ->
                val monthly = offers.firstOrNull { it.pricingPhases.pricingPhaseList.firstOrNull()?.billingPeriod?.contains("P1M") == true }
                val monthlyPrice = monthly?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                premiumPriceMonthly.text = monthlyPrice ?: ""

                val yearly = offers.firstOrNull { it.pricingPhases.pricingPhaseList.firstOrNull()?.billingPeriod?.contains("P1Y") == true }
                val yearlyPrice = yearly?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                premiumPriceYearly.text = getString(R.string.per_year_price_format, yearlyPrice ?: "")
            }

            // ----- BASIC -----
            val basic = productDetailsMap["basic_subscription_id"]
            basic?.subscriptionOfferDetails?.let { offers ->
                val monthly = offers.firstOrNull { it.pricingPhases.pricingPhaseList.firstOrNull()?.billingPeriod?.contains("P1M") == true }
                val monthlyPrice = monthly?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                basicPriceMonthly.text = monthlyPrice ?: ""

                val yearly = offers.firstOrNull { it.pricingPhases.pricingPhaseList.firstOrNull()?.billingPeriod?.contains("P1Y") == true }
                val yearlyPrice = yearly?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                basicPriceYearly.text = getString(R.string.per_year_price_format, yearlyPrice ?: "")
            }
        }

/*
        var text = view.context.getString(R.string.trial_days_left, daysLeft)
        if (!isTrial) {
            text = view.context.getString(R.string.trial_finished)
        }
*/

/*        title.text = if (isTrial) {
            "נותרו לך $daysLeft ימי ניסיון"
        } else {
            "תקופת הניסיון הסתיימה – שדרג כדי לפתוח את כל הפיצ'רים"
        }*/

       // btnClose.setOnClickListener { dismiss() }

        btnStandard.setOnClickListener {
            billingManager.launchPurchaseFlow(requireActivity(), "standard_subscription_id")
        }

        btnPremium.setOnClickListener {
            billingManager.launchPurchaseFlow(requireActivity(), "premium_subscription_id")
        }

        promoButton.setOnClickListener {
            showPromoCodeDialog(requireContext())
        }

        // BASIC features (מה שיש גם ב-standard וגם ב-premium)
        appSpecificFeatures.filter { it.isStandard && it.isPremium }.forEach { feature ->
            basicFeaturesContainer?.addView(createFeatureTextView(feature.title))
        }

        // Header for PREMIUM
/*        premiumFeaturesContainer?.addView(TextView(requireContext()).apply {
            text = "כל מה שבבסיסי, וגם:"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
                topMargin = 8.dpToPx()
            }
        })*/

        premiumFeaturesContainer?.addView(createFeatureTextView(getString(R.string.everything_in_basic_plus), true))

        // PREMIUM-only features
        appSpecificFeatures.filter { !it.isStandard && it.isPremium }.forEach { feature ->
            premiumFeaturesContainer?.addView(createFeatureTextView(feature.title))
        }

        val crownImage = view.findViewById<ImageView>(R.id.upgrade_plan_crown_image)
        val isRTL = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        if (isRTL) {
            crownImage.scaleX = -1f
        }/* else {
            crownImage.scaleX = -1f // לוודא שבמצב LTR היא בכיוון הנכון
        }*/
        val shakeAnimator = AnimatorInflater.loadAnimator(requireContext(), R.animator.shake)
        shakeAnimator.setTarget(crownImage)
        shakeAnimator.start()

        Handler(Looper.getMainLooper()).postDelayed({
            shakeAnimator.cancel() // או shakeAnimator.end()
        }, 2500)

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        //onResult(SubscriptionResult.Canceled)
    }

    data class FeatureRow(val title: String, val isStandard: Boolean, val isPremium: Boolean)

    private fun createFeatureTextView(title: String, isHeader: Boolean = false): TextView {
        val textView = TextView(requireContext()).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER // מרכז את הטקסט בתוך עצמו
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL // מרכז את ה־TextView בתוך ה־LinearLayout
                topMargin = if (isHeader) 4.dpToPx() else 1.dpToPx()
                bottomMargin = if (isHeader) 4.dpToPx() else 0
            }
        }

        // הגדרת הפונט מתוך res/font/open_sans.xml
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.open_sans)
        textView.setTypeface(typeface, if (isHeader) Typeface.BOLD else Typeface.NORMAL)

        return textView
    }


    /*    private fun createFeatureTextView(title: String): TextView {
            return TextView(requireContext()).apply {
                text = "• $title"
                setTextColor(Color.parseColor("#B0B8C9"))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4.dpToPx()
                }
            }
        }*/

    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    fun fetchAndValidateToken(code: String, onResult: (AccessToken?, String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("access_tokens")
            .whereEqualTo("token", code)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onResult(null, getString(R.string.promo_dialog_token_does_not_exist))
                    return@addOnSuccessListener
                }

                val doc = result.documents.first()

                val active = doc.getBoolean("is_active") ?: false
                val maxUses = doc.getLong("max_uses_per_token") ?: 0
                val tokensAlreadyUsed = doc.getLong("num_of_tokens_already_used") ?: 0
                val expiresAt = doc.getDate("expires_at")
                val now = Date()

                if (!active) {
                    onResult(null, getString(R.string.promo_dialog_token_not_active))
                    return@addOnSuccessListener
                }

                if (tokensAlreadyUsed >= maxUses) {
                    onResult(null, getString(R.string.promo_dialog_code_already_used_too_many_times))
                    return@addOnSuccessListener
                }

                if (expiresAt != null && now.after(expiresAt)) {
                    onResult(null, getString(R.string.promo_dialog_expired))
                    return@addOnSuccessListener
                }

                val usedByRaw: List<*> = doc.get("used_by") as? List<*> ?: emptyList<Any?>()
                val usedBy = usedByRaw.filterIsInstance<String>()

                val accessToken = AccessToken(
                    token = doc.getString("token") ?: "",
                    maxUses = maxUses,
                    tokensAlreadyUsed = tokensAlreadyUsed,
                    createdAt = doc.getDate("created_at") ?: now,
                    expiresAt = expiresAt ?: now,
                    active = active,
                    usedBy = usedBy,
                    accessType = doc.getString("access_type") ?: "basic"
                )

                // עדכון המספר בפיירסטור
                db.collection("access_tokens").document(doc.id)
                    .update("num_of_tokens_already_used", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d("SimplyCall - fetchAndValidateToken", "tokens_already_used updated")
                    }
                    .addOnFailureListener {
                        Log.d("SimplyCall - fetchAndValidateToken", "tokens_already_used update FAILED!")
                    }

                onResult(accessToken, null)
            }
            .addOnFailureListener { e ->
                onResult(null, e.localizedMessage)
            }
    }

   /* fun checkTokenAndProceed(context: Context, code: String) {
        fetchAndValidateToken(code) { token, error ->
            if (token != null) {
                when (token.accessType) {
                    "premium" -> {
                        SettingsStaus.isPremium = true
                    }
                    "basic" -> {
                        openBasicFeatures(context)
                    }
                    else -> {
                        Toast.makeText(context, "סוג גישה לא מוכר", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, error ?: "קוד לא תקף או פג תוקף", Toast.LENGTH_LONG).show()
            }
        }
    }*/

    fun showPromoCodeDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_promo_code, null)

        val editText = dialogView.findViewById<EditText>(R.id.promoCodeEditText)
        val promoConfirmButton = dialogView.findViewById<Button>(R.id.promoConfirmButton)
        val promoCancelButton = dialogView.findViewById<Button>(R.id.promoCancelButton)
        val promoCodeResultsMsg = dialogView.findViewById<TextView>(R.id.promoCodeResultsMsg)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        promoConfirmButton.setOnClickListener {
            promoCodeResultsMsg.text = getString(R.string.dialog_promo_processing)
            fetchAndValidateToken(editText.text.toString()) { token, error ->
                if (token != null) {
                    promoCodeResultsMsg.text = ""
                    if (token.accessType == "basic") {
                        onResult(SubscriptionResult.PromoBasic)
                    }
                    else if (token.accessType == "premium") {
                        onResult(SubscriptionResult.PromoPremium)
                    }

                    // המשך טיפול בטוקן
                    dialog.dismiss() // סוגר אחרי הצלחה אם רוצים
                } else {
                    promoCodeResultsMsg.text = error ?: getString(R.string.promo_dialog_code_not_valid)
                    //Toast.makeText(context, error ?: "קוד לא תקף", Toast.LENGTH_LONG).show()
                }
            }
        }

        promoCancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
