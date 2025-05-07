package com.nirotem.simplecall.ui.tourPremium

data class TourStep(
    val key: String,
    val title: String,           // כותרת הצעד
    val description: String,     // תיאור הצעד
    val imageResId: Int?         // מזהה התמונה שתשמש עבור הצעד
)
