package com.nirotem.simplecall.ui.tourPremium

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class PageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        // אפקט של טשטוש
        page.alpha = 1 - Math.abs(position)

        // אם הדף נמצא קצת במרחק מה (למשל, דף צדדי), נקטין אותו
        val scaleFactor = Math.max(0.85f, 1 - Math.abs(position))
        page.scaleX = scaleFactor
        page.scaleY = scaleFactor

        // אם הדף בתצוגה מלאה, נעדכן את מיקומו
        page.translationX = page.width * -position
    }
}
