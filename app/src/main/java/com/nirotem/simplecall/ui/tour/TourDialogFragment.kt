package com.nirotem.simplecall.ui.tour

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadUpdatedTourCaption
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveUpdatedTourCaption
import com.nirotem.simplecall.helpers.SharedPreferencesCache.setTourShown
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.OpenScreensStatus.shouldUpdateSettingsScreens

class TourDialogFragment : DialogFragment(), TourFragment.TourListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Remove the default title for the dialog
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar)
        isCancelable = false  // מונע סגירה דרך back או לחיצה מחוץ לדיאלוג
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the container layout
        return inflater.inflate(R.layout.fragment_tour_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Embed the TourFragment into this DialogFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.tour_dialog_container, TourFragment())
            .commit()
    }

    override fun onStart() {
        super.onStart()
        // Set the dialog's dimensions
        dialog?.window?.let { window ->
            val metrics = resources.displayMetrics
            val width = (metrics.widthPixels * 0.9).toInt()
            val height = (metrics.heightPixels * 0.78).toInt()
            window.setLayout(width, height)
            window.setBackgroundDrawableResource(android.R.color.transparent) // Optional: Make background transparent
        }
    }

    override fun onTourCompleted() {
        val context = requireContext()
        setTourShown(context)
        val updatedVersionCaption = loadUpdatedTourCaption(context)
        if (updatedVersionCaption != null) { // delete it
            saveUpdatedTourCaption(null, context) // we show version update caption only once when the app loads
        }
        if (!OpenScreensStatus.isSettingsScreenOpened) {
            OpenScreensStatus.registerSettingsInstanceValue =
                (if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) OpenScreensStatus.shouldCloseSettingsScreens.value else 0)!!
            val navController = requireActivity().findNavController(R.id.nav_host_fragment_content_main)
            navController.navigate(R.id.nav_settings)
        }
        else {
            shouldUpdateSettingsScreens.value = true
        }
        dismiss()
    }
}
