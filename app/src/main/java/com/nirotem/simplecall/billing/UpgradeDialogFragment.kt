package com.nirotem.simplecall.billing

import android.animation.AnimatorInflater
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import com.nirotem.simplecall.R

class UpgradeDialogFragment(
    private val billingManager: BillingManager,
    private val isTrial: Boolean,
    private val daysLeft: Int,
    private val onDismissed: () -> Unit = {}
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
            (resources.displayMetrics.heightPixels * 0.88).toInt()
        )
       // dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_dark_rounded_background)
        //dialog?.window?.setDimAmount(0.6f)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
       // val btnClose = view.findViewById<View>(R.id.upgrade_dialog_close_button)
        val btnStandard = view.findViewById<Button>(R.id.btnBasic)
        val btnPremium = view.findViewById<Button>(R.id.btnPremium)
        val basicPriceMonthly = view.findViewById<TextView>(R.id.basicPriceMonthly)
        val basicPriceYearly = view.findViewById<TextView>(R.id.basicPriceYearly)
        val premiumPriceMonthly = view.findViewById<TextView>(R.id.premiumPriceMonthly)
        val premiumPriceYearly = view.findViewById<TextView>(R.id.premiumPriceYearly)
/*        val premiumOriginalPrice = view.findViewById<TextView>(R.id.premiumOriginalPrice)
        premiumOriginalPrice.apply {
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
        val basicOriginalPrice = view.findViewById<TextView>(R.id.basicOriginalPrice)
        basicOriginalPrice.apply {
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }*/


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

        var text = view.context.getString(R.string.trial_days_left, daysLeft)
        if (!isTrial) {
            text = view.context.getString(R.string.trial_finished)
        }

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

        val features = listOf(
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_call_management), true, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_big_buttons_icons), true, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_click_to_answer), true, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_gold_number), true, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_quick_call), true, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_lock_screen), false, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_upgraded_quick_call), false, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_start_with_speaker), false, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_auto_answer), false, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_talk_instead_of_ringtone), false, true),
            FeatureRow(view.context.getString(R.string.subscription_plan_feature_name_open_whatsapp), false, true)
        )

        // BASIC features (מה שיש גם ב-standard וגם ב-premium)
        features.filter { it.isStandard && it.isPremium }.forEach { feature ->
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
        features.filter { !it.isStandard && it.isPremium }.forEach { feature ->
            premiumFeaturesContainer?.addView(createFeatureTextView(feature.title))
        }

        val crownImage = view.findViewById<ImageView>(R.id.upgrade_plan_crown_image)
        if (crownImage.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            crownImage.scaleX = 1f
        } else {
            crownImage.scaleX = -1f // לוודא שבמצב LTR היא בכיוון הנכון
        }
        val shakeAnimator = AnimatorInflater.loadAnimator(requireContext(), R.animator.shake)
        shakeAnimator.setTarget(crownImage)
        shakeAnimator.start()

        Handler(Looper.getMainLooper()).postDelayed({
            shakeAnimator.cancel() // או shakeAnimator.end()
        }, 3000)

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissed()
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
}
