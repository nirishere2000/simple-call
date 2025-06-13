package com.nirotem.simplecall.ui.waitingCall

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.R
import com.nirotem.simplecall.WaitingCall
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveCallActivityLoadedTimeStamp
//import com.nirotem.simplecall.databinding.FragmentWaitingCallBinding
import com.nirotem.simplecall.managers.SoundPoolManager
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.ui.components.StandaloneDialerDialogFragment

class WaitingCallFragment : DialogFragment() {
   // private lateinit var textViewPhoneNumber: TextView
   // private lateinit var textViewContactName: TextView
 //   private var _binding: FragmentWaitingCallBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
  //  private val binding get() = _binding!!

/*    companion object {
        private const val ARG_DATA = "CALLER_NUMBER"

        fun newInstance(phoneNumberOrContact: String): WaitingCallFragment {
            val fragment = WaitingCallFragment()
            val args = Bundle().apply {
                putString(ARG_DATA, phoneNumberOrContact)
            }
            fragment.arguments = args
            return fragment
        }
    }*/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_waiting_call, container, false)

        saveCallActivityLoadedTimeStamp(view.context) // fragment could be loaded

        // Get the phone number from the arguments (passed from the activity)
        val phoneNumber = WaitingCall.phoneNumberOrContact // arguments?.getString("CALLER_NUMBER") ?: root.context.getString(R.string.unknown_caller)

        // Use ViewModelFactory to create the ViewModel
        /*val waitingCallViewModel = ViewModelProvider(this,
            WaitingCallViewModel.WaitingCallViewModelFactory(phoneNumber.toString())
        ).get(WaitingCallViewModel::class.java)*/
       val textViewPhoneNumber = view.findViewById<TextView>(R.id.text_incoming_call_contact)
       // val textViewContactName = view.findViewById(R.id.text_incoming_call_label)
        textViewPhoneNumber.text = phoneNumber

        val waitingCallAppIcon = view.findViewById<ImageView>(R.id.waiting_call_app_icon)
        if (SettingsStatus.isPremium) {
            waitingCallAppIcon.setImageResource(SettingsStatus.appLogoResourceSmall)
        }

        //textViewPhoneNumber = binding.textIncomingCallContact
        //textViewContactName = binding.textIncomingCallLabel

/*        val textView: TextView = binding.textIncomingCallContact
        waitingCallViewModel.text.observe(viewLifecycleOwner) {
            textView.text = phoneNumber
        }*/
        val declineButtonImage = view.findViewById<AppCompatImageButton>(R.id.declineButtonImage)
        declineButtonImage.setOnClickListener { // decline call and hide screen
            clickDeclineButton()
        }
        val declineButtonText = view.findViewById<TextView>(R.id.declineButtonText)
        declineButtonText.setOnClickListener { // decline call and hide screen
            clickDeclineButton()
        }

        val acceptButtonImage = view.findViewById<AppCompatImageButton>(R.id.acceptButtonImage)
        acceptButtonImage.setOnClickListener { // decline call and hide screen
            clickAcceptButton()
        }
        val acceptButtonText = view.findViewById<TextView>(R.id.acceptButtonText)
        acceptButtonText.setOnClickListener { // decline call and hide screen
            clickAcceptButton()
        }

        return view
    }



    private fun clickAcceptButton() {
        Log.d("SimplyCall - IncomingWaitingCallFragment", "call was accepted by user! Swiping calls")
        /*            val videoState = if (supportsVideoCall()) {
                        VideoProfile.STATE_BIDIRECTIONAL
                    } else {
                        VideoProfile.STATE_AUDIO_ONLY
                    }*/
       // stopRingtone(false)

        WaitingCall.answer(VideoProfile.STATE_AUDIO_ONLY)

        /*val overlayDialog = parentFragmentManager.findFragmentByTag("WaitingCallWindowDialogFragmentTag") as? WaitingCallFragment
        overlayDialog?.dismiss()*/

// replace number and play call replaced phone sound

/*        exitTransition = android.transition.Fade() // Add fade effect
        parentFragmentManager.beginTransaction()
            .remove(this)  // Hide the current fragment
            .commit()*/
    }

    private fun clickDeclineButton() {
        Log.d("SimplyCall - IncomingWaitingCallFragment", "callRejected button clicked - Fragment")

       // stopRingtone(true)
        WaitingCall.hangup() // we only hang up here (and listen in ActiveCallFragment)
/*        exitTransition = android.transition.Fade() // Add fade effect
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()*/

/*        val overlayDialog = parentFragmentManager.findFragmentByTag("WaitingCallWindowDialogFragmentTag") as? WaitingCallFragment
        overlayDialog?.dismiss()*/
    }

    override fun onStart() {
        super.onStart()

        // Set the dialog to occupy only 50% of the screen's width and height
        val dialog = dialog ?: return
        val window = dialog.window ?: return

        window.setLayout(
            (resources.displayMetrics.widthPixels * 1).toInt(), // 80% of the screen width
            (resources.displayMetrics.heightPixels * 1).toInt() // 50% of the screen height
        )

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Optional: Transparent background
        window.setGravity(Gravity.CENTER) // Center the dialog
    }

    private fun getSoundToPlayName(isCallWaiting: Boolean): String {
        val soundToPlay = if (isCallWaiting) SoundPoolManager.incomingCallWaitingSoundName else SoundPoolManager.incomingCallSoundName
        return soundToPlay
    }

    private fun stopRingtone(isCallWaiting: Boolean) {
        val soundToStop = getSoundToPlayName(isCallWaiting)
        SoundPoolManager.stopSound(soundToStop)
    }

    // Checks if the call supports video
    private fun supportsVideoCall(): Boolean {
        val capabilities = OngoingCall.call?.details?.callCapabilities
        return (capabilities?.and(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_TX) != 0) &&
                (capabilities?.and(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_RX) != 0)
    }
}