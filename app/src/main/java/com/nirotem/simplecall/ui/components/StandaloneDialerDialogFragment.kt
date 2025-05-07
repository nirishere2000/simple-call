package com.nirotem.simplecall.ui.components

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

import androidx.fragment.app.DialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.R


class StandaloneDialerDialogFragment : DialogFragment() {
    private var textEnteredNumber: TextView? = null
    private var isOutgoingCall = false
    private val buttonPressedImaged = R.drawable.digitkeybuttonpressedtransparent
    private val originalButtonImage = R.drawable.digitkeybuttontransparent

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_standalone_dialer, container, false)
        isOutgoingCall = arguments?.getBoolean("IS_OUT_GOING", false) == true

        val keypadLayout = view.findViewById<LinearLayout>(R.id.numbersKeypadBack)

        val key0 = keypadLayout.findViewById<ConstraintLayout>(R.id.key0Button)
//        val key0ButtonImage = view.findViewById<ImageView>(R.id.key0ButtonImage)
        val key1 = keypadLayout.findViewById<ConstraintLayout>(R.id.key1Button)
 //       val key1ButtonImage = view.findViewById<ImageView>(R.id.key1ButtonImage)
        val key2 = keypadLayout.findViewById<ConstraintLayout>(R.id.key2Button)
 //       val key2ButtonImage = view.findViewById<ImageView>(R.id.key2ButtonImage)
        val key3 = keypadLayout.findViewById<ConstraintLayout>(R.id.key3Button)
   //     val key3ButtonImage = view.findViewById<ImageView>(R.id.key3ButtonImage)
        val key4 = keypadLayout.findViewById<ConstraintLayout>(R.id.key4Button)
 //       val key4ButtonImage = view.findViewById<ImageView>(R.id.key4ButtonImage)
        val key5 = keypadLayout.findViewById<ConstraintLayout>(R.id.key5Button)
  //      val key5ButtonImage = view.findViewById<ImageView>(R.id.key5ButtonImage)
        val key6 = keypadLayout.findViewById<ConstraintLayout>(R.id.key6Button)
  //      val key6ButtonImage = view.findViewById<ImageView>(R.id.key6ButtonImage)
        val key7 = keypadLayout.findViewById<ConstraintLayout>(R.id.key7Button)
     //   val key7ButtonImage = view.findViewById<ImageView>(R.id.key7ButtonImage)
        val key8 = keypadLayout.findViewById<ConstraintLayout>(R.id.key8Button)
     //   val key8ButtonImage = view.findViewById<ImageView>(R.id.key8ButtonImage)
        val key9 = keypadLayout.findViewById<ConstraintLayout>(R.id.key9Button)
     //   val key9ButtonImage = view.findViewById<ImageView>(R.id.key9ButtonImage)
    //    val addNewContactButton = view.findViewById<ImageView>(R.id.addNewContactButton)
        val poundKey = keypadLayout.findViewById<ConstraintLayout>(R.id.keyPoundButton)
    //    val keyPoundButtonImage = view.findViewById<ImageView>(R.id.keyPoundButtonImage)
        val asteriskKey = keypadLayout.findViewById<ConstraintLayout>(R.id.asteriskKeyButton)
     //   val asteriskKeyButtonImage = view.findViewById<ImageView>(R.id.asteriskKeyButtonImage)

        val closeButton = view.findViewById<TextView>(R.id.standaloneCloseButton)
        textEnteredNumber = view.findViewById(R.id.standaloneTextEnteredNumber)

        Log.d("SimplyCall - StandaloneDialerDialogFragment", "StandaloneDialerFragment - Loading...")


        key0.setOnClickListener {
            clickKey('0')

            Log.d("SimplyCall - StandaloneDialerDialogFragment", "StandaloneDialerFragment digit 0 clicked")
        }

        key1.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()

            clickKey('1')

        }


        key2.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('2')

        }

        key3.setOnClickListener {
            clickKey('3')

        }

        key4.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('4')

        }
        key5.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('5')

        }

        key6.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('6')

        }

        key7.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('7')

        }

        key8.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('8')

        }

        key9.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('9')

        }

        asteriskKey.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('*')
        }

        poundKey.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            clickKey('#')
        }

        closeButton.setOnClickListener {
            val overlayDialog = parentFragmentManager.findFragmentByTag("StandaloneDialerDialogFragmentTag") as? StandaloneDialerDialogFragment
            overlayDialog?.dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        // Set the dialog to occupy only 50% of the screen's width and height
        val dialog = dialog ?: return
        val window = dialog.window ?: return

        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.86).toInt(), // 80% of the screen width
            (resources.displayMetrics.heightPixels * 0.78).toInt() // 70% of the screen height
        )

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Optional: Transparent background
        window.setGravity(Gravity.CENTER) // Center the dialog
    }

    private fun clickKey(keyChar: Char) {
        //  val digit = button.text.toString()

        // change image or text back for clicking animation
/*        buttonImage.setImageResource(buttonPressedImaged)
        Handler(Looper.getMainLooper()).postDelayed({
            buttonImage.setImageResource(originalButtonImage)
        }, 200) // Revert after 200ms*/

        if (textEnteredNumber !== null) {
            //  SoundPoolManager.playSound(SoundPoolManager.keyClickSoundName)
            val text =
                if (textEnteredNumber?.text !== null) textEnteredNumber?.text.toString() else ""
            val newString = text + keyChar
            textEnteredNumber!!.text = newString
        }
        sendSignalToCall(keyChar)

        Log.d("SimplyCall - DialerFragment", "DialerFragment digit $keyChar clicked")
    }

    private fun sendSignalToCall(sign: Char) {
        try {
            if (isOutgoingCall) {
                OutgoingCall.call?.playDtmfTone(sign)

                // Stop playing the tone after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    OutgoingCall.call?.stopDtmfTone()
                }, 200) // Adjust the delay as needed
            }
            else {
                OngoingCall.call?.playDtmfTone(sign)

                // Stop playing the tone after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    OngoingCall.call?.stopDtmfTone()
                }, 200) // Adjust the delay as needed
            }


            Log.d("SimplyCall - StandaloneDialerFragment", "StandaloneDialerFragment sent $sign to call (isOutgoing = $isOutgoingCall)")

        }
        catch (err: Error) {
            Log.d("SimplyCall - StandaloneDialerFragment", "StandaloneDialerFragment ERROR! Sent $sign to call (Error $err)")
        }
    }
}