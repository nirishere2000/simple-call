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

data class AccessToken(
    val token: String,
    val maxUses: Long,
    val tokensAlreadyUsed: Long,
    val createdAt: Date,
    val expiresAt: Date,
    val active: Boolean,
    val usedBy: List<String>,
    val accessType: String,
    val appId: String
)