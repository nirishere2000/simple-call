package com.nirotem.simplecall.ui.dialer

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.READ_CONTACTS
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.SoundPoolManager

class DialerFragment : Fragment(R.layout.fragment_dialer) {
    private var textEnteredNumber: TextView? = null
    private var makingMakingCallPermission = false
    private val buttonPressedImaged = R.drawable.digitkeybuttonpressedtransparent
    private val originalButtonImage = R.drawable.digitkeybuttontransparent
    private val keypadPressedImaged = R.drawable.digitkeyclickedbluefulltransparent
    private val originalKeypadImage = R.drawable.digitkeytransparent
    private var shouldPlayDigitsSound = false

    // Declare the permission request launcher
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (makingMakingCallPermission) { // from now on we have making call permission
                    PermissionsStatus.callPhonePermissionGranted.value = true
                    makingMakingCallPermission = false
                }
/*                Toast.makeText(
                    this.requireContext(),
                    "Permission was granted! Please try again",
                    Toast.LENGTH_SHORT
                ).show()*/
                showCustomToastDialog(context,
                    getString(R.string.permission_was_granted_please_try_again))
            } else {
/*                Toast.makeText(
                    this.requireContext(),
                    "Cannot continue since permission was not approved",
                    Toast.LENGTH_SHORT
                ).show()*/
                showCustomToastDialog(context, getString(R.string.cannot_continue_since_permission_was_not_approved))
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shouldPlayDigitsSound = view.context.resources.getBoolean(R.bool.playDigitsSound)
        val isPremium = resources.getBoolean(R.bool.isAdvancedDialer)
        if (isPremium) {
            // Hide the FAB in this fragment
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            fab?.visibility = View.GONE
        }

        val key0 = view.findViewById<ConstraintLayout>(R.id.key0Button)
        val key0ButtonImage = view.findViewById<ImageView>(R.id.key0ButtonImage)
        val key1 = view.findViewById<ConstraintLayout>(R.id.key1Button)
        val key1ButtonImage = view.findViewById<ImageView>(R.id.key1ButtonImage)
        val key2 = view.findViewById<ConstraintLayout>(R.id.key2Button)
        val key2ButtonImage = view.findViewById<ImageView>(R.id.key2ButtonImage)
        val key3 = view.findViewById<ConstraintLayout>(R.id.key3Button)
        val key3ButtonImage = view.findViewById<ImageView>(R.id.key3ButtonImage)
        val key4 = view.findViewById<ConstraintLayout>(R.id.key4Button)
        val key4ButtonImage = view.findViewById<ImageView>(R.id.key4ButtonImage)
        val key5 = view.findViewById<ConstraintLayout>(R.id.key5Button)
        val key5ButtonImage = view.findViewById<ImageView>(R.id.key5ButtonImage)
        val key6 = view.findViewById<ConstraintLayout>(R.id.key6Button)
        val key6ButtonImage = view.findViewById<ImageView>(R.id.key6ButtonImage)
        val key7 = view.findViewById<ConstraintLayout>(R.id.key7Button)
        val key7ButtonImage = view.findViewById<ImageView>(R.id.key7ButtonImage)
        val key8 = view.findViewById<ConstraintLayout>(R.id.key8Button)
        val key8ButtonImage = view.findViewById<ImageView>(R.id.key8ButtonImage)
        val key9 = view.findViewById<ConstraintLayout>(R.id.key9Button)
        val key9ButtonImage = view.findViewById<ImageView>(R.id.key9ButtonImage)
        val addNewContactButton = view.findViewById<ImageView>(R.id.addNewContactButton)
        val poundKey = view.findViewById<ConstraintLayout>(R.id.keyPoundButton)
        val keyPoundButtonImage = view.findViewById<ImageView>(R.id.keyPoundButtonImage)
        val asteriskKey = view.findViewById<ConstraintLayout>(R.id.asteriskKeyButton)
        val asteriskKeyButtonImage = view.findViewById<ImageView>(R.id.asteriskKeyButtonImage)
        val backDeleteButton = view.findViewById<ConstraintLayout>(R.id.backDeleteButton)
        val backDeleteButtonImage = view.findViewById<ImageView>(R.id.backDeleteButtonImage)
        val dialerCallButton = view.findViewById<ImageView>(R.id.dialerCallButton)
        val plusKeyPadButton = view.findViewById<ConstraintLayout>(R.id.plusKeyPadButton)
        val plusKeyPadButtonImage = view.findViewById<ImageView>(R.id.plusKeyPadButtonImage)
        val deleteAllButton = view.findViewById<ConstraintLayout>(R.id.deleteAllKeyPadButton)
        val deleteAllKeyPadImage = view.findViewById<ImageView>(R.id.deleteAllKeyPadImage)
        val goldNumberButton = view.findViewById<ImageView>(R.id.goldNumberButton)
        textEnteredNumber = view.findViewById<TextView>(R.id.textEnteredNumber)

        key0.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit0SoundName)
            }

            clickKey("0", key0ButtonImage)

            Log.d("SimplyCall - DialerFragment", "DialerFragment digit 2 clicked")
        }

        key1.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit1SoundName)
            }
            clickKey("1", key1ButtonImage)

        }


        key2.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit2SoundName)
            }
            clickKey("2", key2ButtonImage)

        }

        key3.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit3SoundName)
            }
            clickKey("3", key3ButtonImage)

        }

        key4.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit4SoundName)
            }
            clickKey("4", key4ButtonImage)

        }
        key5.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit5SoundName)
            }
            clickKey("5", key5ButtonImage)

        }

        key6.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit6SoundName)
            }
            clickKey("6", key6ButtonImage)

        }

        key7.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit7SoundName)
            }
            clickKey("7", key7ButtonImage)

        }

        key8.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit8SoundName)
            }
            clickKey("8", key8ButtonImage)

        }

        key9.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.digit9SoundName)
            }
            clickKey("9", key9ButtonImage)

        }

        asteriskKey.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.asteriskSoundName)
            }
            clickKey("*", asteriskKeyButtonImage)
        }

        poundKey.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            if (shouldPlayDigitsSound) {
                SoundPoolManager.playSound(SoundPoolManager.poundKeySoundName)
            }
            clickKey("#", keyPoundButtonImage)
        }

        plusKeyPadButton.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            plusKeyPadButtonImage.setImageResource(buttonPressedImaged)
            Handler(Looper.getMainLooper()).postDelayed({
                plusKeyPadButtonImage.setImageResource(originalButtonImage)
            }, 200) // Revert after 200ms
            clickKey("+", null)
        }

        backDeleteButton.setOnClickListener {
            backDeleteButtonImage.setImageResource(buttonPressedImaged)
            Handler(Looper.getMainLooper()).postDelayed({
                backDeleteButtonImage.setImageResource(originalButtonImage)
            }, 200) // Revert after 200ms
            // Handle button click
            //  val digit = button.text.toString()
            clickBackDelete()
        }

        deleteAllButton.setOnClickListener {
            deleteAllKeyPadImage.setImageResource(buttonPressedImaged)
            Handler(Looper.getMainLooper()).postDelayed({
                deleteAllKeyPadImage.setImageResource(originalButtonImage)
            }, 200) // Revert after 200ms
            textEnteredNumber!!.text = ""
        }

        dialerCallButton.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            makePhoneCallFromKeyPadText()
        }

        goldNumberButton.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            callGoldNumber()
        }

        addNewContactButton.setOnClickListener {
            addContact()
        }
    }

    private fun clickKey(keyChar: String, buttonImage: ImageView?) {
        buttonImage?.setImageResource(keypadPressedImaged)
        Handler(Looper.getMainLooper()).postDelayed({
            buttonImage?.setImageResource(originalKeypadImage)
        }, 200) // Revert after 200ms
        if (textEnteredNumber !== null) {
            val text =
                if (textEnteredNumber?.text !== null) textEnteredNumber?.text.toString() else ""
            val newString = text + keyChar
            textEnteredNumber!!.text = newString
        }

        Log.d("SimplyCall - DialerFragment", "DialerFragment digit $keyChar clicked")
    }

    private fun clickBackDelete() {
        // Removing the last digit
        val currentText = textEnteredNumber?.text.toString()
        if (currentText.isNotEmpty()) {
            textEnteredNumber!!.text = currentText.dropLast(1)
        }
    }

    private fun makePhoneCallFromKeyPadText() {
        val currPhoneNumber = textEnteredNumber?.text.toString()
        if (currPhoneNumber.isNotEmpty()) {
            makeCall(currPhoneNumber)
        }
    }

    private fun makeCall(callPhoneNumber: String) {
        //   if (checkSelfPermission(this, CALL_PHONE) == PERMISSION_GRANTED) {
        //val uri = "tel:${+97237537900}".toUri()
        val context = requireContext()

        val uri = "tel:${callPhoneNumber}".toUri()

        if (PermissionsStatus.callPhonePermissionGranted.value !== null && PermissionsStatus.callPhonePermissionGranted.value!!) {
            // We have permissions - make the call:
            OutgoingCall.isCalling = true
            ContextCompat.startActivity(context, Intent(Intent.ACTION_CALL, uri), null)
        } else { // ask for permissions:
            if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), CALL_PHONE)
            ) {
                showPermissionsExplanationDialog()
            } else {
                makingMakingCallPermission = true
                showPermissionsConfirmationDialog(
                    context.getString(R.string.permission_needed_capital_p),
                    "In order to make calls, the application must have the proper permission",
                    ::requestCallPhonePermission
                )
            }
        }

        //     } else {
        //        requestPermissions(this, arrayOf(CALL_PHONE), REQUEST_PERMISSION)
        //      }
    }

    fun showPermissionsExplanationDialog() {
        val message = """
            How to Re-enable The Phone Permission in Android Manually

It looks like you’ve denied the permission for the app to make phone calls. To continue using the full features of the app, you need to manually enable this permission. Follow these steps to grant the required permission:

Open Settings:
Go to the Settings app on your phone.

Select "Permissions":
Scroll down and open the Permissions or Apps Settings option (the name may vary depending on your device).

Go to "Permission Manager":
Inside the app settings, look for the Permission Manager option (could be called "Permissions").

Select "Phone":
Under phone permissions, tap on Phone.

Find the App:
In the list of apps, find the app for which you need to enable the permission.

Enable the Permission:
Once you find the app, tap on it and select Allow or Grant to enable the permission.

Exit Settings:
After enabling the permission, exit the settings. You should now be able to use the app without restrictions.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Re-enable Permission")
            .setMessage(message)
            .setPositiveButton(requireContext().getString(R.string.close_capital)) { dialog, _ ->
                dialog.dismiss() // Close the dialog
            }
            .setCancelable(false) // Make the dialog not cancellable by touching outside
            .show()
    }

    private fun requestCallPhonePermission() {
        requestPermissionLauncher.launch(CALL_PHONE)
    }

    private fun callGoldNumber() {
        val sharedPreferences =
            this.context?.getSharedPreferences("SimpleCallPreferences", Context.MODE_PRIVATE)
        val goldPhoneNumber = sharedPreferences?.getString("gold_phone_number", null)

        if (goldPhoneNumber !== null && goldPhoneNumber !== "") {
            makeCall(goldPhoneNumber)
        } else { // load gold number dialog to insert a gold number
            val navController = findNavController()  // Get the NavController
            navController.navigate(R.id.action_firstFragment_to_secondFragment)
        }
    }

    private fun addContact() {
        val currPhoneNumber = textEnteredNumber?.text.toString()
        val bundle = Bundle().apply {
            putString("phone_number", currPhoneNumber)
        }
        val navController = findNavController()
        navController.navigate(R.id.action_dialer_to_editContact, bundle)
    }

    private fun showPermissionsConfirmationDialog(
        msgTitle: String,
        msgText: String,
        onAskPermission: () -> Unit
    ) {
        val context = this.requireContext()
        val builder = AlertDialog.Builder(context)

        builder.setTitle(msgTitle)
        builder.setMessage(msgText)

        builder.setPositiveButton(context.getString(R.string.ask_permission_capital_a)) { dialog, which ->
            onAskPermission()
        }

        builder.setNegativeButton(context.getString(R.string.cancel_capital)) { dialog, which ->
            dialog.dismiss()
        }

        // יצירת והצגת הדיאלוג
        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val isPremium = resources.getBoolean(R.bool.isAdvancedDialer)
        if (isPremium) {
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            // Hide the FAB in this fragment
            fab?.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        if (PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.readContactsPermissionGranted.value = true
               // Toast.makeText(context, "Read Contacts permission was granted! Please try again", Toast.LENGTH_SHORT).show()
                showCustomToastDialog(context,
                    getString(R.string.read_contacts_permission_was_granted_please_try_again))
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
        }
        if (PermissionsStatus.callPhonePermissionGranted.value === null || (!(PermissionsStatus.callPhonePermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.callPhonePermissionGranted.value = true
                makingMakingCallPermission = false
/*                Toast.makeText(context,
                    getString(R.string.make_call_permission_was_granted_please_try_again), Toast.LENGTH_SHORT).show()*/
                showCustomToastDialog(context, getString(R.string.make_call_permission_was_granted_please_try_again))
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
        }
    }
}