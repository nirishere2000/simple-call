package com.nirotem.simplecall.ui.conferenceCall

import android.content.ContentValues.TAG
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.R
import com.nirotem.simplecall.databinding.FragmentConferenceCallBinding

class ConferenceCallFragment : Fragment() {
    private lateinit var textViewPhoneNumber: TextView
    private var isSpeakerOn: Boolean = false
    private var _binding: FragmentConferenceCallBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConferenceCallBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Get the phone number from the arguments (passed from the activity)
        val phoneNumber = arguments?.getString("CALLER_NUMBER") ?: root.context.getString(R.string.unknown_caller)
        val phoneNumber2 = arguments?.getString("CALLER_NUMBER2") ?: root.context.getString(R.string.unknown_caller)

        // Use ViewModelFactory to create the ViewModel
        val incomingCallViewModel = ViewModelProvider(this,
            ConferenceCallViewModel.ConferenceCallViewModelFactory(phoneNumber)
        ).get(ConferenceCallViewModel::class.java)



        textViewPhoneNumber = binding.textConferenceCallContact
        val contactLabel = binding.root.findViewById<AppCompatTextView>(R.id.text_conference_call_contact)
        contactLabel.text = phoneNumber
        val contactLabel2 = binding.root.findViewById<AppCompatTextView>(R.id.text_conference_call_contact2)
        contactLabel2.text = phoneNumber2
       // textViewContactName = binding.textIncomingCallLabel

/*        val textView: TextView = binding.textIncomingCallContact
        incomingCallViewModel.text.observe(viewLifecycleOwner) {
            textView.text = phoneNumber
        }*/
        val declineButton = binding.root.findViewById<AppCompatImageButton>(R.id.declineButton)

        declineButton.setOnClickListener { // decline call and hide screen
            Log.d("SimplyCall - ActiveCallFragment", "declineButton clicked - Fragment")
            OngoingCall.hangup()
            exitTransition = android.transition.Fade() // Add fade effect
            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        val speakerButton = binding.root.findViewById<AppCompatImageButton>(R.id.speakerButton)
        if (isSpeakerphoneOn(this.requireContext())) {
            speakerButton.setImageResource(R.drawable.speaker_on)
        }

        speakerButton.setOnClickListener { // decline call and hide screen
            if (this.context !== null) {
             //   Log.d("SimplyCall - ActiveCallFragment", "set Speaker before: (isSpeakerOn = $isSpeakerOn), (isSpeakerphoneOn(this.requireContext()) = ${isSpeakerphoneOn(this.requireContext())}")
                if (isSpeakerOn) {
                    setSpeakerphone(this.requireContext(), false)
                    speakerButton.setImageResource(R.drawable.speaker)
                }
                else {
                    setSpeakerphone(this.requireContext(), true)
                    speakerButton.setImageResource(R.drawable.speaker_on)
                }
                    // Log.d("SimplyCall - ActiveCallFragment", "set Speaker after: (isSpeakerOn = $isSpeakerOn), (isSpeakerphoneOn(this.requireContext()) = ${isSpeakerphoneOn(this.requireContext())}")
            }
        }
        return root
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateText(newPhoneOrContact: String) {
       // val contactLabel = binding.root.findViewById<AppCompatTextView>(R.id.text_active_call_contact)
       // contactLabel.text = newPhoneOrContact
        //textViewPhoneNumber = newPhoneOrContact
        // not implemented yet
    }

    /**
     * פונקציה להדלקה או כיבוי של הרמקול (Speaker).
     * פועלת על גרסאות Android 12+ בעזרת setCommunicationDevice,
     * ובגרסאות ישנות יותר בעזרת isSpeakerphoneOn.
     *
     * @param context קונטקסט
     * @param enable  true אם רוצים להדליק את הרמקול, false אם לכבות
     */
    private fun setSpeakerphone(context: Context, enable: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // נבקש AudioFocus, כדי שהמערכת תדע שאנחנו רוצים לנהל כרגע את ערוץ האודיו
        val focusResult = audioManager.requestAudioFocus(
            /* listener = */ null,
            /* streamType = */ AudioManager.STREAM_VOICE_CALL,
            /* durationHint = */ AudioManager.AUDIOFOCUS_GAIN
        )
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Audio focus not granted. Can't toggle speakerphone.")
            return
        }

        // נגדיר את מצב האודיו כדי לנהל שיחה.
        // כברירת מחדל באפליקציות Dialer רבות נהוג להשתמש ב-MODE_IN_CALL,
        // אבל אם אתה מוצא בעיות, ניתן לנסות MODE_IN_COMMUNICATION.
        audioManager.mode = AudioManager.MODE_IN_CALL

        // כדי לעקוב אחר מה שקורה בלוג:
        Log.d(
            TAG, "setSpeakerphone(enable=$enable) >> Before: " +
                "mode=${audioManager.mode}, " +
                "isSpeakerphoneOn=${audioManager.isSpeakerphoneOn}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 ומעלה:
            val speakerDevice = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

            if (enable) {
                if (speakerDevice != null) {
                    val success = audioManager.setCommunicationDevice(speakerDevice)
                    if (!success) {
                        Log.e(TAG, "setCommunicationDevice(speaker) failed!")
                    } else {
                        Log.d(TAG, "setCommunicationDevice(speaker) succeeded.")
                    }
                } else {
                    // אם לא נמצאה יציאה לרמקול, אולי יש בעיה במכשיר
                    Log.e(TAG, "No built-in speaker device found!")
                }
            } else {
                // החזרת המצב להתקן ברירת המחדל (אוזניה/אפרכסת וכו')
                audioManager.clearCommunicationDevice()
                Log.d(TAG, "clearCommunicationDevice called.")
            }
        } else {
            // גרסאות ישנות יותר:
            audioManager.isSpeakerphoneOn = enable
            Log.d(TAG, "isSpeakerphoneOn set to $enable (legacy API).")
        }

        // לוג אחרי שהגדרנו
        Log.d(
            TAG, "setSpeakerphone(enable=$enable) >> After: " +
                "mode=${audioManager.mode}, " +
                "isSpeakerphoneOn=${audioManager.isSpeakerphoneOn}")
    }

    /**
     * פונקציה לבדיקה אם הרמקול דולק כרגע.
     * עבור אנדרואיד 12 ומעלה, נבדוק אם ה-CommunicationDevice הנוכחי הוא הרמקול המובנה.
     * בגרסאות ישנות יותר, נתבסס על AudioManager.isSpeakerphoneOn.
     *
     * @param context קונטקסט
     * @return true אם הרמקול דולק, אחרת false
     */
    private fun isSpeakerphoneOn(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val currentDevice = audioManager.communicationDevice
            // אם ה-CommunicationDevice הוא רמקול
            return currentDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            // גרסאות ישנות: פשוט בודקים את המתג של speakerphone
            return audioManager.isSpeakerphoneOn
        }
    }

    private fun toggleSpeakerphone() {
        isSpeakerOn = !isSpeakerOn
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = isSpeakerOn // Fallback for older versions
            Log.d(
                "SimplyCall - EventScreenActivity",
                "Turn audioManager.isSpeakerphoneOn = $audioManager.isSpeakerphoneOn (isSpeakerOn = $isSpeakerOn)"
            )
        } else {
            modernToggleSpeakerphone(isSpeakerOn)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun modernToggleSpeakerphone(enable: Boolean) {
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (enable) {
            Log.d(
                "SimplyCall - EventScreenActivity",
                "Turn audioManager true, $audioManager.isSpeakerphoneOn = $audioManager.isSpeakerphoneOn"
            )

            val devices = audioManager.availableCommunicationDevices
            val speakerDevice = devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speakerDevice != null) {
                audioManager.setCommunicationDevice(speakerDevice)
            } else {
                Log.e("SpeakerControl", "Speakerphone device not found!")
            }
            Log.d(
                "SimplyCall - EventScreenActivity",
                "Turn audioManager true (after), $audioManager.isSpeakerphoneOn = $audioManager.isSpeakerphoneOn"
            )
        } else {
            Log.d(
                "SimplyCall - EventScreenActivity",
                "Turn audioManager false, $audioManager.isSpeakerphoneOn = $audioManager.isSpeakerphoneOn"
            )
            audioManager.clearCommunicationDevice()
            Log.d(
                "SimplyCall - EventScreenActivity",
                "Turn audioManager false (after), $audioManager.isSpeakerphoneOn = $audioManager.isSpeakerphoneOn"
            )
        }

    }
}