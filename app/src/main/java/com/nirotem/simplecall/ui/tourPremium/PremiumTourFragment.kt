package com.nirotem.simplecall.ui.tourPremium

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.nirotem.simplecall.R
import com.nirotem.simplecall.adapters.PremiumTourAdapter
import com.nirotem.simplecall.statuses.OpenScreensStatus

class PremiumTourFragment : Fragment() {

    private var wasGuideShown = false
    private lateinit var premiumTourAdapter: PremiumTourAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var steps: List<TourStep>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var rootView = inflater.inflate(R.layout.fragment_premium_tour, container, false)

        try {
            OpenScreensStatus.isPremiumTourScreenOpened = true

            viewPager = rootView.findViewById(R.id.viewPager)


            // הוספת אנימציה
            viewPager.setPageTransformer(PageTransformer())

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    // 1. קושרים ל-RecyclerView של ה-ViewPager
                    val rv = viewPager.getChildAt(0) as? RecyclerView ?: return

                    // 2. מוצאים את ה-ViewHolder בעמדה הנוכחית
                    val holder = rv.findViewHolderForAdapterPosition(position)
                            as? PremiumTourAdapter.TourViewHolder ?: return

                    // 3. מרעננים את ה-UI של העמוד (גלילה, voice commands וכו')
                    holder.bind(steps[position], position)

                    // 4. מתמודדים עם ה-swipe guide
                    val swipeGuide = holder.itemView
                        .findViewById<FrameLayout>(R.id.swipeGuideLayout)
                    if (position > 0 || wasGuideShown) {
                        wasGuideShown = true
                        swipeGuide.visibility = View.GONE
                    } else {
                        swipeGuide.visibility = View.VISIBLE
                        Handler(Looper.getMainLooper()).postDelayed({
                            swipeGuide.animate()
                                .alpha(0f)
                                .setDuration(3500) // חצי שנייה דהייה
                                .withEndAction {
                                    swipeGuide.visibility = View.GONE
                                 //   swipeGuide.alpha = 0.5f // לאפס את השקיפות חזרה לערך קודם, אם תשתמש שוב
                                }
                        }, 3500)

                    }
                }
            })


      /*      viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    val rv = viewPager.getChildAt(0) as? RecyclerView ?: return
                    (rv.findViewHolderForAdapterPosition(position) as? PremiumTourAdapter.TourViewHolder)
                        ?.bind(steps[position], position)

                    val recyclerView = viewPager.getChildAt(0) as? RecyclerView
                    val holder = recyclerView.findViewHolderForAdapterPosition(position) as? PremiumTourAdapter.TourViewHolder
                    holder?.bind(steps[position], position)

                    val currentViewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                    val swipeGuideLayout = currentViewHolder?.itemView?.findViewById<FrameLayout>(R.id.swipeGuideLayout)

                    if (position > 0 || wasGuideShown) {
                        wasGuideShown = true
                        swipeGuideLayout?.visibility = View.GONE
                    }
                    else {
                        swipeGuideLayout?.visibility = View.VISIBLE
                    }

                    // אם עברנו מהעמוד הראשון — נסתיר את ההדר פעם אחת
                    *//*   if (!wasGuideShown) {
       *//**//*                    swipeGuideLayout?.animate()?.alpha(0f)?.setDuration(500)?.withEndAction {
                        swipeGuideLayout.visibility = View.GONE
                    }?.start()*//**//*

                    wasGuideShown = true
                }*//*
                }
            })*/

            steps = listOf(
                TourStep("welcome",// key (should not be translated!)
                    getString(R.string.premium_tour_page_welcome_title),
                    getString(R.string.premium_tour_page_welcome_text),
                    R.drawable.goldnumbercall
                ),
                TourStep("distress",// key (should not be translated!)
                    getString(R.string.premium_tour_page_distress_button_title),
                    getString(R.string.premium_tour_page_distress_button_text),
                    null
                ),
                TourStep("lock",// key (should not be translated!)
                    getString(R.string.premium_tour_page_lock_title),
                    getString(R.string.premium_tour_page_lock_text),
                    R.drawable.unlock_screen
                ),
                TourStep("voiceIntro",// key (should not be translated!)
                    getString(R.string.premium_tour_page_voice_title),
                    getString(R.string.premium_tour_page_voice_text),
                    R.drawable.phonevoice
                ),
                TourStep("voice",// key (should be translated!)
                    getString(R.string.premium_tour_page_voice_commands_title),
                    getString(R.string.premium_tour_page_voice_commands_text),
                    null
                )
              /*  TourStep("report", // key (should be translated!)
                    getString(R.string.premium_tour_page_distress_button_title),
                    getString(R.string.premium_tour_page_distress_button_text),
                    R.drawable.showonlockscreenpermission),*/
            )

            premiumTourAdapter = PremiumTourAdapter(steps, requireActivity())
            //val adapter = PremiumTourAdapter(steps)

            viewPager.adapter = premiumTourAdapter
            viewPager.offscreenPageLimit = steps.size

            if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) {
                OpenScreensStatus.shouldCloseSettingsScreens.value = OpenScreensStatus.shouldCloseSettingsScreens.value!! + 1
            }
            if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) {
                OpenScreensStatus.shouldClosePermissionsScreens.value = OpenScreensStatus.shouldClosePermissionsScreens.value!! + 1
            }

            OpenScreensStatus.shouldClosePremiumTourScreens.observe(viewLifecycleOwner) { currInstance ->
                if (currInstance != null && currInstance > OpenScreensStatus.registerPremiumTourInstanceValue) {
                    parentFragmentManager.popBackStack()
                }
            }
        }
        catch (e: Exception) {
            Log.e("SimplyCall - PremiumTourFragment", "onCreateView Error (${e.message})")
        }

        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            OpenScreensStatus.isPremiumTourScreenOpened = false
            premiumTourAdapter.onDestroy()
            //    konfettiView.stopGracefully()
        } catch (e: Exception) {
            Log.e("SimplyCall - PremiumTourFragment", "onDestroy Error (${e.message})")
        }
    }
}
