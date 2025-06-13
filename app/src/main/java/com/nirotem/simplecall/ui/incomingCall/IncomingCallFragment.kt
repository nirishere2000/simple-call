package com.nirotem.simplecall.ui.incomingCall

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.VideoProfile
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nirotem.simplecall.CallActivity
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.R
import com.nirotem.simplecall.VoiceApiImpl
import com.nirotem.simplecall.databinding.FragmentIncomingCallBinding
import com.nirotem.simplecall.helpers.DBHelper.getContactIdFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getContactPhoto
import com.nirotem.simplecall.managers.SoundPoolManager
import com.nirotem.simplecall.managers.VoiceApi
import com.nirotem.simplecall.statuses.SettingsStatus

class IncomingCallFragment : Fragment() {
    private lateinit var textViewPhoneNumber: TextView
    private lateinit var textIncomingCallLabel: TextView
    private var userReacted = false
    private var isAutoAnswer = false
    private var _binding: FragmentIncomingCallBinding? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateTitleRunnable: Runnable? = null
    private val voiceApi: VoiceApi = VoiceApiImpl()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        private const val ARG_DATA = "CALLER_NUMBER"
        private const val ARGS_AUTO_ANSWER = "AUTO_ANSWER"

        fun newInstance(phoneNumberOrContact: String, isAutoAnswer: Boolean): IncomingCallFragment {
            val fragment = IncomingCallFragment()
            val args = Bundle().apply {
                putString(ARG_DATA, phoneNumberOrContact)
                putBoolean(ARGS_AUTO_ANSWER, isAutoAnswer)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomingCallBinding.inflate(inflater, container, false)
        val root: View = binding.root

        Log.d("SimplyCall - IncomingCallFragment", "Fragment loaded")

        try {

            if (voiceApi.isEnabled()) {
                voiceApi.updateLastCommand("")
                voiceApi.getLastCommand().observe(viewLifecycleOwner) {
                    val spokeCommand = voiceApi.getLastCommand().value

                    if (spokeCommand != null) {
                        val indexAnswerLocal = spokeCommand.lastIndexOf(getString(R.string.answer_capital))
                        val indexAnswerEnglish = spokeCommand.lastIndexOf("Answer") // also allow English
                        val indexAnswer = maxOf(indexAnswerLocal, indexAnswerEnglish)
                        val indexDeclineLocal = spokeCommand.lastIndexOf(getString(R.string.decline_capital))
                        val indexDeclineEnglish = spokeCommand.lastIndexOf("Decline") // also allow English
                        val indexDecline = maxOf(indexDeclineLocal, indexDeclineEnglish)

                        if (indexAnswer >= 0 && indexAnswer > indexDecline) {
                            clickAcceptButton()
                        }
                        else if (indexDecline >= 0) {
                            clickDeclineButton()
                        }
                    }
                }
                voiceApi.startListenToVoiceCommands(root.context)
            }
        }
        catch (e: Exception) {
            Log.e(
                "SimplyCall - IncomingCallFragment",
                "SpeakCommandsManager startListen Error (${e.message})"
            )
        }

        // Get the phone number from the arguments (passed from the activity)
        val phoneNumber = arguments?.getString("CALLER_NUMBER") ?: root.context.getString(R.string.unknown_caller)
        val isAutoAnswer = arguments?.getBoolean("AUTO_ANSWER") == true

        // Use ViewModelFactory to create the ViewModel
        val incomingCallViewModel = ViewModelProvider(this,
            IncomingCallViewModel.IncomingCallViewModelFactory(phoneNumber)
        ).get(IncomingCallViewModel::class.java)

        textViewPhoneNumber = binding.textIncomingCallContact
        textIncomingCallLabel = binding.textIncomingCallLabel

        //val context = binding.root.context

        try { // try to set Contact's image, if exists. Otherwise, put App logo.
            val context = binding.root.context
            val contactId = if (CallActivity.originalPhoneNumber != null) getContactIdFromPhoneNumber(
                context,
                CallActivity.originalPhoneNumber!!
            ) else null

            val contactOrAppIcon = binding.incomingCallAppImage
            if (SettingsStatus.isPremium) {
                contactOrAppIcon.setImageResource(SettingsStatus.appLogoResourceSmall)
            }
            val contactExistingPhotoBack = binding.contactExistingPhotoBack

            if (contactId != null) {
                val photoImageView = binding.photoImageView
                val userProfilePicture = getContactPhoto(
                    context,
                    contactId
                ) // if contactId is not null then we know fragmentContext isn't null
                if (userProfilePicture != null) {
                    photoImageView.setImageBitmap(userProfilePicture)
                    contactExistingPhotoBack.visibility = VISIBLE
                    contactOrAppIcon.visibility = GONE
                } else {
                    //contactOrAppIcon.set
                    contactExistingPhotoBack.visibility = GONE
                    contactOrAppIcon.visibility = VISIBLE
                }
            }
            else {
                contactExistingPhotoBack.visibility = GONE
                contactOrAppIcon.visibility = VISIBLE
            }
        }
        catch (e: Exception) {
            Log.e("SimplyCall - IncomingCallFragment", "try to set Contact's image - but got error (${e.message})")
        }

        val textView: TextView = binding.textIncomingCallContact
        incomingCallViewModel.text.observe(viewLifecycleOwner) {
            textView.text = phoneNumber
        }
        val declineButtonImage = binding.root.findViewById<AppCompatImageButton>(R.id.declineButtonImage)
        declineButtonImage.setOnClickListener { // decline call and hide screen
            clickDeclineButton()
        }
        val declineButtonText = binding.root.findViewById<TextView>(R.id.declineButtonText)
        declineButtonText.setOnClickListener { // decline call and hide screen
            clickDeclineButton()
        }

        val acceptButtonImage = binding.root.findViewById<AppCompatImageButton>(R.id.acceptButtonImage)
        acceptButtonImage.setOnClickListener {
            clickAcceptButton()
        }
        val acceptButtonText = binding.root.findViewById<TextView>(R.id.acceptButtonText)
        acceptButtonText.setOnClickListener {
            clickAcceptButton()
        }

        OngoingCall.autoAnwered.observe(viewLifecycleOwner) { isAutoAnswered ->
            if (isAutoAnswered) { // was answered not from here, but we still need to close the form
                clickAcceptButton()
            }
        }

        try {
            // if for some reason we are not closed when phone call answered/disconnected we run a timer to close form
            // For example when app is not in focus and missing run in background or lock screen permissions - maybe this could happen
            updateTitleRunnable = Runnable {
                Log.d("SimplyCall - IncomingCallFragment", "Runnable trying to close form")
                closeForm()
            }
            Log.d("SimplyCall - IncomingCallFragment", "Runnable started")
            handler.postDelayed(updateTitleRunnable!!, 40000) // 100000 מילישניות = 40 שניות
        }
        catch (e: Exception) {
            Log.e("SimplyCall - IncomingCallFragment", "try to run a timer to close form - but got error (${e.message})")
        }

        return root
    }

    private fun clickDeclineButton() {
        //Toast.makeText(context, "Hello", Toast.LENGTH_SHORT).show()
        Log.d("SimplyCall - IncomingCallFragment", "callRejected button clicked - Fragment")
        userReacted = true
        voiceApi.stopListen()
        //  stopRingtone(true)

        OngoingCall.hangup()
        closeForm()

/*        exitTransition = android.transition.Fade() // Add fade effect
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()*/

        /*            incomingCallViewModel.rejectIncomingCall()
                    parentFragmentManager.beginTransaction()
                        .remove(this)  // Hide the current fragment
                        .commit()*/
    }

    private fun clickAcceptButton() {
        Log.d("SimplyCall - IncomingCallFragment", "Incoming call was accepted by user!")
        userReacted = true
        //SpeakCommandsManager.stopListen()
        OngoingCall.answer(VideoProfile.STATE_AUDIO_ONLY) // which should put OngoingCall on hold
        textIncomingCallLabel.text = getString(R.string.answering_capital)
        // Just replacing Incoming Call with Answering..
        //closeForm()
    }

    private fun closeForm() {
        //exitTransition = android.transition.Fade() // Add fade effect
        parentFragmentManager.beginTransaction()
            .remove(this)  // Hide the current fragment
            .commit()
        requireActivity().finish()
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
      //  stopRingtone(false)
     //   stopRingtone(true)
        try {
            updateTitleRunnable?.let { handler.removeCallbacks(it) }
            voiceApi.stopListen()
        }
        catch (e: Exception) {
            Log.e("SimplyCall - IncomingCallFragment", "onDestroyView 1 - got error (${e.message})")
        }

        try {
            if (!userReacted && !isAutoAnswer) { // if we are closing and user did not click any button (but not auto answer)
                //  then we must manually disconnect the call
                OngoingCall.hangup()
            }
        }
        catch (e: Exception) {
            Log.e("SimplyCall - IncomingCallFragment", "onDestroyView 2 - got error (${e.message})")
        }

        _binding = null
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