package com.nirotem.subscription

import android.animation.AnimatorInflater
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

sealed class SubscriptionResult {
    object PromoBasic : SubscriptionResult()
    object PromoPremium : SubscriptionResult()
    object SubscribedBasic : SubscriptionResult()
    object SubscribedPremium : SubscriptionResult()
    object Canceled : SubscriptionResult()
    data class Error(val message: String) : SubscriptionResult()
}
