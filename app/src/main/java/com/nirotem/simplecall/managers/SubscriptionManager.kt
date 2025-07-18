package com.nirotem.simplecall.managers


import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.nirotem.sharedmodules.statuses.AppData.APP_ID_EASY_CALL_AND_ANSWER_BASIC
import com.nirotem.sharedmodules.statuses.AppData.APP_ID_EASY_CALL_AND_ANSWER_PREMIUM
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.statuses.SettingsStatus.isPremium
import com.nirotem.subscription.SubscriptionResult
import com.nirotem.subscription.UpgradeDialogFragment

object SubscriptionManager {
    fun showTrialBanner(context: Context, fragmentManager: FragmentManager, onResult: () -> Unit) {
        val dialog = UpgradeDialogFragment(
            APP_ID_EASY_CALL_AND_ANSWER_BASIC,
            APP_ID_EASY_CALL_AND_ANSWER_PREMIUM,
            SettingsStatus.appFeatures,
            onResult = { result ->
                if (result == SubscriptionResult.PromoPremium ||
                    result == SubscriptionResult.PromoBasic ||
                    result == SubscriptionResult.SubscribedPremium ||
                    result == SubscriptionResult.SubscribedBasic) {
                    val upgradeDialog = fragmentManager.findFragmentByTag("UpgradeDialog") as? UpgradeDialogFragment
                    upgradeDialog?.dismiss()
                }
                handleSubscriptionResult(result, context, onResult)
            }
        )
        dialog.show(fragmentManager, "UpgradeDialog")
    }

    private fun handleSubscriptionResult(result: SubscriptionResult, context: Context, onResult: () -> Unit) {
        when (result) {
            is SubscriptionResult.PromoBasic -> openFeatures(false, true, context, onResult)
            is SubscriptionResult.PromoPremium -> openFeatures(true, true, context, onResult)
            is SubscriptionResult.SubscribedBasic -> openFeatures(false, false, context, onResult)
            is SubscriptionResult.SubscribedPremium -> openFeatures(true, false, context, onResult)
            is SubscriptionResult.Canceled -> {
                Toast.makeText(context, context.getString(com.nirotem.subscription.R.string.subscription_canceled), Toast.LENGTH_LONG).show()
            }
            is SubscriptionResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openFeatures(isPremiumAccess: Boolean, isPromoCode: Boolean, context: Context, onResult: () -> Unit) {
        SettingsStatus.lockedBecauseTrialIsOver = false
        isPremium = isPremiumAccess
        if (isPremium && isPromoCode) {
            Toast.makeText(context, context.getString(com.nirotem.subscription.R.string.subscription_premium_code_approved), Toast.LENGTH_LONG).show()
        }
        else if (!isPremium && isPromoCode) {
            Toast.makeText(context, context.getString(com.nirotem.subscription.R.string.subscription_basic_code_approved), Toast.LENGTH_LONG).show()
        }
        else if (isPremium) {
            Toast.makeText(context, context.getString(com.nirotem.subscription.R.string.subscription_premium_subscription_approved), Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(context, context.getString(com.nirotem.subscription.R.string.subscription_basic_subscription_approved), Toast.LENGTH_LONG).show()
        }

        onResult()
    }
}
