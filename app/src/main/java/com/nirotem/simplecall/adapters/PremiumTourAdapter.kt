package com.nirotem.simplecall.adapters

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nirotem.simplecall.R
import com.nirotem.simplecall.managers.VoiceManager.initVoiceCommandsSettings
import com.nirotem.simplecall.statuses.PermissionsStatus.askForRecordPermission
import com.nirotem.simplecall.ui.tourPremium.TourStep

class PremiumTourAdapter(private val steps: List<TourStep>, activity: Activity) : RecyclerView.Adapter<PremiumTourAdapter.TourViewHolder>() {

    private lateinit var distressButtonBack: LinearLayout
    private lateinit var distressButtonIcon: ImageView
    private lateinit var distressButtonTextDisabled: TextView
    private lateinit var scrollContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var scrollArrow: ImageView
    private var activityForPermissions = activity
   // private lateinit var gradientView: View

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TourViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_premium_tour_step, parent, false)
       // initVoiceCommands(view)
        return TourViewHolder(view)
    }

    override fun onBindViewHolder(holder: TourViewHolder, position: Int) {
        val step = steps[position]
        holder.bind(step, position)
    }

    override fun getItemCount(): Int = steps.size

    inner class TourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //private val swipeGuideLayout: FrameLayout = itemView.findViewById(R.id.swipeGuideLayout)
        private val commandsContainer: View = itemView.findViewById(R.id.commandsContainer)
        private val distressButtonBack: LinearLayout = itemView.findViewById(R.id.premium_distress_button_image)
        private val distressButtonIcon: ImageView = itemView.findViewById(R.id.emergency_button_icon)
        private val distressButtonTextDisabled: TextView = itemView.findViewById(R.id.quick_call_button_text_disabled)
        private val scrollContainer: LinearLayout = itemView.findViewById(R.id.premium_tour_scroll_arrow_container)
        private val scrollView: ScrollView = itemView.findViewById(R.id.premium_tour_scrollable_text)
        private val scrollArrow: ImageView = itemView.findViewById(R.id.premium_tour_scroll_arrow)

        fun bind(step: TourStep, position: Int) {
            // כותרת, תיאור ותמונה
            itemView.findViewById<TextView>(R.id.tourTitle).text = step.title
            itemView.findViewById<TextView>(R.id.tourDescription).text = step.description
            step.imageResId?.let {
                itemView.findViewById<ImageView>(R.id.premium_tour_image).apply {
                    setImageResource(it)
                    visibility = View.VISIBLE
                }
            } ?: run {
                itemView.findViewById<ImageView>(R.id.premium_tour_image).visibility = View.GONE
            }

            // הצגת voice-commands רק ב־step.key=="voice"
            commandsContainer.visibility = if (step.key == "voice") VISIBLE else GONE

            if (step.key == "voice") {
                val commandToggleAnswerCalls = itemView.findViewById<SwitchMaterial>(R.id.command_toggle_answer_calls)
                val commandToggleGoldNumber = itemView.findViewById<SwitchMaterial>(R.id.command_toggle_gold_number)
                val commandToggleUnlockScreen = itemView.findViewById<SwitchMaterial>(R.id.command_toggle_unlock_screen)
                val commandToggleDistressButton = itemView.findViewById<SwitchMaterial>(R.id.command_toggle_distress_button)

                val atLeastOnceCommandEnabled = commandToggleAnswerCalls.isChecked || commandToggleGoldNumber.isChecked
                        || commandToggleDistressButton.isChecked || commandToggleUnlockScreen.isChecked

                if (atLeastOnceCommandEnabled) {
                    askForRecordPermission(itemView.context, activityForPermissions)
                }

                initVoiceCommandsSettings(itemView, activityForPermissions)
            }

            distressButtonBack.visibility = if (step.key == "distress") VISIBLE else GONE
            distressButtonIcon.setImageResource(R.drawable.ic_bell_gold)
            distressButtonIcon.visibility = if (step.key == "distress") VISIBLE else GONE // making sure the button look is enabled for the Tour display
            distressButtonTextDisabled.visibility = GONE // making sure the button look is enabled for the Tour display

            // אתחול גלילה
            scrollView.scrollTo(0, 0)
            scrollView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    checkScroll()
                }
            })
            scrollView.postDelayed({ checkScroll() }, 50)
            scrollView.setOnScrollChangeListener { _, _, _, _, _ -> checkScroll() }

            // אנימציה חיה לחץ
            scrollArrow.startAnimation(AnimationUtils.loadAnimation(itemView.context, R.anim.arrow_fade))
        }

        private fun checkScroll() {
            if (scrollView.canScrollVertically(1)) {
                if (scrollArrow.animation == null) {
                    scrollArrow.startAnimation(AnimationUtils.loadAnimation(itemView.context, R.anim.blink))
                }
                scrollContainer.visibility = VISIBLE
            } else {
                scrollArrow.clearAnimation()
                scrollContainer.visibility = GONE
            }
        }

        fun cleanUp() {
            // קוראים ב־onDestroy של ה־Adapter או כשיוצאים מהדף
            scrollView.setOnScrollChangeListener(null)
        }
    }

    private fun checkScroll(context: Context) {
        if (scrollView.canScrollVertically(1)) {
            if (scrollArrow.animation == null) {
                val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)
                scrollArrow.startAnimation(blinkAnimation)
            }
            scrollContainer.visibility = View.VISIBLE
        } else {
            scrollArrow.clearAnimation()
            scrollContainer.visibility = View.GONE
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
