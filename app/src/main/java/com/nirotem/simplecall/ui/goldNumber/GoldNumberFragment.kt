package com.nirotem.simplecall.ui.goldNumber

import android.Manifest.permission.READ_CONTACTS
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.databinding.FragmentGoldNumberBinding
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog

class GoldNumberFragment : Fragment() {

    private var _binding: FragmentGoldNumberBinding? = null
    private var askingReadContactsPermission = false
    // Declare the permission request launcher
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var fragmentRoot: View

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {
        private const val ARG_DATA = "GOLD_NUMBER"

        fun newInstance(phoneNumberOrContact: String): GoldNumberFragment {
            val fragment = GoldNumberFragment()
            val args = Bundle().apply {
                putString(ARG_DATA, phoneNumberOrContact)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (askingReadContactsPermission) { // from now on we have making call permission
                    askingReadContactsPermission = false
                    PermissionsStatus.readContactsPermissionGranted.value = true
                    loadContactsAutoComplete()
                }
/*                Toast.makeText(
                    context,
                    "Permission was granted! Please try again",
                    Toast.LENGTH_SHORT
                ).show()*/
                showCustomToastDialog(context, getString(R.string.permission_was_granted_please_try_again))
            } else {
/*                Toast.makeText(
                    context,
                    "Cannot continue since permission was not approved",
                    Toast.LENGTH_SHORT
                ).show()*/
                showCustomToastDialog(context, getString(R.string.cannot_continue_since_permission_was_not_approved))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoldNumberBinding.inflate(inflater, container, false)
        val root: View = binding.root
        fragmentRoot = root

        val autoCompleteTextView: AutoCompleteTextView = root.findViewById(R.id.goldContactsAutoComplete)
        val closeGoldNumberWindow = root.findViewById<ImageView>(R.id.closeGoldNumberWindow)
        val saveGoldNumberButton = root.findViewById<ImageView>(R.id.saveGoldNumberButton)
        val goldNumberExistingContactChip = root.findViewById<Chip>(R.id.goldNumberExistingContactChip)
        val goldNumberPhoneNumberChip = root.findViewById<Chip>(R.id.goldNumberPhoneNumberChip)
        val goldNumberPhoneNumberText = root.findViewById<EditText>(R.id.goldPhoneNumberText)
        var autoCompleteContactPhoneNumber = ""

        if (PermissionsStatus.readContactsPermissionGranted.value !== null && PermissionsStatus.readContactsPermissionGranted.value!!) {
            // We have Read Contacts permission - can read contacts
            val contacts =  getContacts(root.context)

            val adapter = ArrayAdapter(
                root.context,
                android.R.layout.simple_dropdown_item_1line,
                contacts
            )

            autoCompleteTextView.setAdapter(adapter)
        }
        else { // No Read Contacts permission:
            // Disable Autocomplete and ask for permissions
            autoCompleteTextView.isEnabled = false
            autoCompleteTextView.isFocusable = false
            autoCompleteTextView.isFocusableInTouchMode = false
            goldNumberExistingContactChip.isEnabled = false
            goldNumberExistingContactChip.isCheckable = false

            if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), READ_CONTACTS)
            ) { // User rejected permission
                showPermissionsExplanationDialog()
            } else {
                askingReadContactsPermission = true
                showPermissionsConfirmationDialog(
                    root.context.getString(R.string.permission_needed_capital_p),
                    "In order to read Contacts details, the application must have the proper permission",
                    ::requestReadContactPermission
                )
            }
        }

        // Set up the listener to extract the phone number
        autoCompleteTextView.setOnItemClickListener { _, view, position, _ ->
            val selectedItem = (view as TextView).text.toString()

            // Extract the phone number using regex
            val phoneNumber = Regex("<(.*?)>").find(selectedItem)?.groups?.get(1)?.value

            // Do something with the phone number
            autoCompleteContactPhoneNumber = ""
            phoneNumber?.let {
                autoCompleteContactPhoneNumber = it
            }
        }


        // Set a listener for state changes (checked/unchecked)
        goldNumberExistingContactChip.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Chip is selected
               goldNumberPhoneNumberChip.isChecked = false
            } else {
                // Chip is deselected
               // goldNumberPhoneNumberChip.isChecked = true
            }
        }

        // Set a listener for state changes (checked/unchecked)
        goldNumberPhoneNumberChip.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Chip is selected
               goldNumberExistingContactChip.isChecked = false
            } else {
                // Chip is deselected
              //  goldNumberExistingContactChip.isChecked = true
            }
        }

        saveGoldNumberButton.setOnClickListener {
            // Extract Gold Number to save:
            var goldPhoneNumberToSave = ""
            if (goldNumberExistingContactChip.isChecked && autoCompleteContactPhoneNumber.isNotEmpty()) {
                goldPhoneNumberToSave = autoCompleteContactPhoneNumber
            }
            else if (goldNumberPhoneNumberChip.isChecked && goldNumberPhoneNumberText.text.isNotEmpty()) {
                goldPhoneNumberToSave = goldNumberPhoneNumberText.text.toString()
            }
            // Save the phone Number
            if (goldPhoneNumberToSave.isNotEmpty()) {
                savePhoneNumber(root.context, goldPhoneNumberToSave)
                exitTransition = android.transition.Fade() // Add fade effect
                parentFragmentManager.popBackStack()  // This will remove the current fragment and return to the previous one
            }
        }

        closeGoldNumberWindow.setOnClickListener {
            exitTransition = android.transition.Fade() // Add fade effect
            parentFragmentManager.popBackStack()  // This will remove the current fragment and return to the previous one
        }

        return root
    }

    // Function to save phone number in SharedPreferences
    fun savePhoneNumber(context: Context, phoneNumber: String) {
        // Obtain SharedPreferences instance
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("SimpleCallPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save the phone number with a key
        editor.putString("gold_phone_number", phoneNumber)
        editor.apply() // Apply changes asynchronously
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getContacts(context: Context): List<String> {
        val contactsList = mutableListOf<String>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phone = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                contactsList.add("$name - $phone")
            }
        }

        return contactsList
    }

    private fun showPermissionsExplanationDialog() {
        val message = """
            How to Re-enable The Contacts Permission in Android Manually

It looks like you’ve denied the permission for the app to view Contacts. To continue using the full features of the app, you need to manually enable this permission. Follow these steps to grant the required permission:

Open Settings:
Go to the Settings app on your phone.

Select "Permissions":
Scroll down and open the Permissions or Apps Settings option (the name may vary depending on your device).

Go to "Permission Manager":
Inside the app settings, look for the Permission Manager option (could be called "Permissions").

Select "Contacts".

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

        val dialog = builder.create()
        dialog.show()
    }

    private fun requestReadContactPermission() {
        requestPermissionLauncher.launch(READ_CONTACTS)
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
                //Toast.makeText(context, "Read Contacts permission was granted!", Toast.LENGTH_SHORT).show()
                showCustomToastDialog(context, getString(R.string.read_contacts_permission_was_granted_reloading))

                loadContactsAutoComplete()
            } //else {
        }
    }

    private fun loadContactsAutoComplete() {
        val contacts =  getContacts(fragmentRoot.context)

        val adapter = ArrayAdapter(
            fragmentRoot.context,
            android.R.layout.simple_dropdown_item_1line,
            contacts
        )

        val autoCompleteTextView: AutoCompleteTextView = fragmentRoot.findViewById(R.id.goldContactsAutoComplete)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.isEnabled = true
        autoCompleteTextView.isFocusable = true
        autoCompleteTextView.isFocusableInTouchMode = true
        val goldNumberExistingContactChip = fragmentRoot.findViewById<Chip>(R.id.goldNumberExistingContactChip)
        goldNumberExistingContactChip.isEnabled = true
        goldNumberExistingContactChip.isCheckable = true
    }
}