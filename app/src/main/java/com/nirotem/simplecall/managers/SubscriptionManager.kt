package com.nirotem.simplecall.managers



import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.statuses.SettingsStatus.isPremium
import com.nirotem.subscription.SubscriptionResult
import com.nirotem.subscription.UpgradeDialogFragment

object SubscriptionManager {

    fun showTrialBanner(context: Context, fragmentManager: FragmentManager, onResult: () -> Unit) {
        val dialog = UpgradeDialogFragment(
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
                Toast.makeText(context, "הרכישה בוטלה", Toast.LENGTH_LONG).show()
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
            Toast.makeText(context, "Premium code approved!", Toast.LENGTH_LONG).show()
        }
        else if (!isPremium && isPromoCode) {
            Toast.makeText(context, "Basic code approved!", Toast.LENGTH_LONG).show()
        }
        else if (isPremium) {
            Toast.makeText(context, "Premium subscription approved!", Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(context, "Basic subscription approved!", Toast.LENGTH_LONG).show()
        }

        onResult()
    }
}
