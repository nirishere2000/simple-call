package com.nirotem.simplecall.helpers

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import com.nirotem.simplecall.R
import com.nirotem.simplecall.databinding.FragmentPermissionsAlertBinding
import com.nirotem.simplecall.databinding.SimpleDialogBinding
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.askForCallPhonePermission
import com.nirotem.simplecall.statuses.PermissionsStatus.requestRole
import com.nirotem.simplecall.ui.permissionsScreen.PermissionsAlertFragment

class DialogManager : DialogFragment() {
    private var _binding: SimpleDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var scrollView: ScrollView
    private lateinit var scrollArrow: ImageView
    private lateinit var gradientView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SimpleDialogBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // אתחול רכיבי הדיאלוג
        val tvTitle = root.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = root.findViewById<TextView>(R.id.tvMessage)
        val btnClose = root.findViewById<Button>(R.id.btnClose)

        tvTitle.text = arguments?.getString("title", "Error")
        tvMessage.text = arguments?.getString("text", "Error")
        btnClose.text = arguments?.getString("okButtonCaption", "Error")

        btnClose.setOnClickListener {
            closeForm()
        }

        // Scroll:
        scrollView = root.findViewById(R.id.scroll_view)
        scrollArrow = root.findViewById(R.id.scroll_arrow)
        gradientView = root.findViewById(R.id.gradient_view)

        scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            scrollView.post {
                checkScroll(scrollView.context)
            }
        }

        scrollView.postDelayed({
            checkScroll(root.context)
        }, 50)

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            checkScroll(root.context)
        }

/*        scrollView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                checkScroll(root.context)

            }
        })
        // Also reset scroll position
        scrollView.scrollTo(0, 0)*/

        return root
    }



    private fun closeForm() {
        val overlayDialog = parentFragmentManager.findFragmentByTag("DialogManagerTag") as? DialogManager
        overlayDialog?.dismiss()
    }

    override fun onStart() {
        super.onStart()

        // Set the dialog to occupy only 50% of the screen's width and height
        val dialog = dialog ?: return
        val window = dialog.window ?: return

        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(), // 85% of the screen width
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Optional: Transparent background
        window.setGravity(Gravity.CENTER) // Center the dialog
    }

    private fun checkScroll(context: Context) {
        if (scrollView.canScrollVertically(1)) {
/*            if (scrollArrow.animation == null) {
                val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)
                scrollArrow.startAnimation(blinkAnimation)
            }
            scrollArrow.visibility = View.VISIBLE
            gradientView.visibility = View.VISIBLE*/
        } else {
            scrollArrow.clearAnimation()
            scrollArrow.visibility = View.GONE
            gradientView.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        scrollView.setOnScrollChangeListener(null)
    }
}

