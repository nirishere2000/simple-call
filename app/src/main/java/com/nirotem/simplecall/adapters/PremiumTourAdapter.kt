package com.nirotem.simplecall.adapters

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nirotem.simplecall.R
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.ui.tourPremium.TourStep
import kotlinx.coroutines.cancel
import java.util.Locale


class PremiumTourAdapter(private val steps: List<TourStep>) : RecyclerView.Adapter<PremiumTourAdapter.TourViewHolder>() {

    private lateinit var distressButtonBack: LinearLayout
    private lateinit var distressButtonIcon: ImageView
    private lateinit var distressButtonTextDisabled: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var scrollArrow: ImageView
    private lateinit var gradientView: View

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TourViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_premium_tour_step, parent, false)
        return TourViewHolder(view)
    }

    override fun onBindViewHolder(holder: TourViewHolder, position: Int) {
        val step = steps[position]
        holder.bind(step, position)
    }

    override fun getItemCount(): Int = steps.size

    inner class TourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val swipeGuideLayout: FrameLayout = itemView.findViewById(R.id.swipeGuideLayout)

        fun bind(step: TourStep, position: Int) {
            itemView.findViewById<TextView>(R.id.tourTitle).text = step.title
            itemView.findViewById<TextView>(R.id.tourDescription).text = step.description
            val stepImage = itemView.findViewById<ImageView>(R.id.tourImage)
            if (step.imageResId != null) {
                stepImage.setImageResource(step.imageResId)
                stepImage.visibility = VISIBLE
            }
            else {
                stepImage.visibility = GONE
            }

            if (step.key == "voice") {
                val commandsContainer = itemView.findViewById<View>(R.id.commandsContainer)
                commandsContainer?.visibility = View.VISIBLE
            }
            else {
                val commandsContainer = itemView.findViewById<View>(R.id.commandsContainer)
                commandsContainer?.visibility = View.GONE
            }

            // distress:
            distressButtonBack = itemView.findViewById(R.id.distressButtonBack)
            distressButtonIcon = itemView.findViewById(R.id.emergency_button_icon)
            distressButtonTextDisabled = itemView.findViewById(R.id.emergency_button_text_disabled)

            if (step.key == "distress") {
                distressButtonBack.visibility = VISIBLE
                distressButtonIcon.visibility = VISIBLE
                distressButtonTextDisabled.visibility = GONE
            }
            else {
                distressButtonBack.visibility = GONE
                distressButtonIcon.visibility = GONE
                distressButtonTextDisabled.visibility = GONE
            }



            val arrowIcon = itemView.findViewById<ImageView>(R.id.arrowIcon)

            val isRTL = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL
            arrowIcon.rotationY = if (isRTL) 0f else 180f

            scrollView = itemView.findViewById(R.id.scrollable_text)
            scrollArrow = itemView.findViewById(R.id.scroll_arrow)
            gradientView = itemView.findViewById(R.id.gradient_view)
            scrollView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    if (itemView.context != null) {
                        checkScroll(itemView.context)
                    }

                }
            })
            // Also reset scroll position
            scrollView.scrollTo(0, 0)

            scrollView.postDelayed({
                checkScroll(itemView.context)
            }, 50)

            scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                checkScroll(itemView.context)
            }

            val arrow = itemView.findViewById<ImageView>(R.id.settings_scroll_arrow)
            val anim = AnimationUtils.loadAnimation(itemView.context, R.anim.arrow_fade)
            arrow.startAnimation(anim)


            /*            if (!alreadySawTooltip) {
                            itemView.findViewById<FrameLayout>(R.id.swipeGuideLayout).visibility = VISIBLE
                            alreadySawTooltip = true
                        }
                        else {
                            itemView.findViewById<FrameLayout>(R.id.swipeGuideLayout).visibility = GONE
                        }*/
        }
    }

    private fun checkScroll(context: Context) {
        if (scrollView.canScrollVertically(1)) {
            if (scrollArrow.animation == null) {
                val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)
                scrollArrow.startAnimation(blinkAnimation)
            }
            scrollArrow.visibility = View.VISIBLE
            gradientView.visibility = View.VISIBLE
        } else {
            scrollArrow.clearAnimation()
            scrollArrow.visibility = View.GONE
            gradientView.visibility = View.GONE
        }
    }

    fun onDestroy() {
        try {
            scrollView.setOnScrollChangeListener(null)
        } catch (e: Exception) {
            Log.e("SimplyCall - PremiumTourAdapter", "onDestroy Error (${e.message})")
        }
    }

}
