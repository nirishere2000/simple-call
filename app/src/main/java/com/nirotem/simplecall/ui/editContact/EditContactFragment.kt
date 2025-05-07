package com.nirotem.simplecall.ui.editContact

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.app.Activity.RESULT_OK
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.databinding.FragmentEditContactBinding
import com.nirotem.simplecall.helpers.DBHelper.getContactPhoto
import com.nirotem.simplecall.helpers.DBHelper.isNumberInFavorites
import com.nirotem.simplecall.ui.permissionsScreen.PermissionsAlertFragment
import com.nirotem.simplecall.ui.singleCallHistory.SingleCallHistoryViewModel
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

class EditContactFragment : Fragment() {
    private var askingReadContactsPermission = false
    private var askingWriteContactsPermission = false
    private var _binding: FragmentEditContactBinding? = null
    private lateinit var addContactPhoto: ImageView
    private lateinit var contactExistingPhotoBack: FrameLayout
    private lateinit var contactExistingPhoto: ImageView
    private lateinit var textAddChangePhoto: TextView
    private var contactId: Long? = null
    private var originalIsContactInFav: Boolean = false
    private var isContactInFav: Boolean = false
    private var originalUserImage: Bitmap? = null
    private lateinit var userImage: Bitmap

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Declare the permission request launcher
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var originalContactName = ""
    private var originalPhoneNumber = ""

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                selectedImageUri?.let { uri ->
                    // addContactPhoto.setImageURI(uri)
                    contactExistingPhoto.setImageURI(uri)
                    addContactPhoto.visibility = GONE
                    contactExistingPhotoBack.visibility = VISIBLE
                    textAddChangePhoto.text = "Change photo"
                    val scale = resources.displayMetrics.density
                    val dpWidth = (120 * scale + 0.5f).toInt() // converts 200dp to pixels
                    val params = textAddChangePhoto.layoutParams
                    params.width = dpWidth
                    textAddChangePhoto.layoutParams = params

                    // במקום לנסות להשיג Real Path, פשוט נפתח Stream ישירות מה-Uri
                    val bitmap =
                        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }

                    if (bitmap != null && contactId != null) {
                        userImage = bitmap
                        enableDisableSave()
                        // שומר את התמונה לאיש הקשר
                        //saveContactPhoto(bitmap, contactId!!)

                        // טוען מחדש ומציג את תמונת איש הקשר ב-ImageView

                        // fetchAndDisplayContactPhoto(contactId!!)

                        // אם תרצו לראות את התמונה מיד בלי לטעון מחדש, פשוט:
                        // imageView.setImageBitmap(bitmap)
                    } else {
                        Log.e(
                            "SimplyCall - EditContactFragment",
                            "pickImageLauncher - Contact ID=$contactId, bitmap=$bitmap"
                        )
                    }
                }
            }
        }


    /* private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
         if (result.resultCode == RESULT_OK) {
             val selectedImageUri: Uri? = result.data?.data
             selectedImageUri?.let { uri ->
                 val realPath = getRealPathFromUri(uri)
                 val bitmap = if (realPath != null) {
                     getBitmapFromUri(realPath)
                     //BitmapFactory.decodeFile(realPath)
                 } else {
                     // Fallback to decode the input stream if path is null
 *//*                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }*//*
                   getBitmapFromUri(uri)
*//*                    requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }*//*
                }

                //val bitmap = getBitmapFromUri(uri)

                // Save the selected image as a contact photo
                if (bitmap != null && contactId !== null) {
                    saveContactPhoto(bitmap, contactId!!)
                    fetchAndDisplayContactPhoto(contactId!!)
                }
                else {
                    Log.e("SimplyCall - EditContactFragment", "pickImageLauncher - Contact ID=$contactId, bitmap=$bitmap)")
                }
            }
        }
    }*/


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
                } else if (askingWriteContactsPermission) {
                    askingWriteContactsPermission = false
                    PermissionsStatus.writeContactsPermissionGranted.value = true
                }
                Toast.makeText(
                    this.requireContext(),
                    "Permission was granted! Please try again",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this.requireContext(),
                    "Cannot continue since permission was not approved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //  companion object {
    //  private const val ARG_DATA = "GOLD_NUMBER"

    /*        fun newInstance(phoneNumberOrContact: String): EditContactFragment {
                val fragment = EditContactFragment()
                val args = Bundle().apply {
                    putString(ARG_DATA, phoneNumberOrContact)
                }
                fragment.arguments = args
                return fragment
            }*/
    //  }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Hide the FAB in this fragment
        val isPremium = resources.getBoolean(R.bool.isAdvancedDialer)
        if (isPremium) {
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            fab?.visibility = View.GONE
        }

        _binding = FragmentEditContactBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val editContactNameText: EditText = root.findViewById(R.id.editContactName)
        val editContactPhoneNumberText: EditText =
            root.findViewById(R.id.editContactPhoneNumberText)
        val saveContactButton = root.findViewById<TextView>(R.id.txtSaveButton)
        val addToFavText = root.findViewById<TextView>(R.id.addToFavText)
        val addToFavImage = root.findViewById<ImageView>(R.id.addToFavImage)
        textAddChangePhoto = root.findViewById(R.id.textAddChangePhoto)

        editContactNameText.addTextChangedListener { editable ->
            enableDisableSave()
            /* if (editable != null) {
                 if (editable.isNotEmpty() && editable.toString() != originalContactName) {
                     saveContactButton.isEnabled =
                         editContactPhoneNumberText.text != null && editContactPhoneNumberText.text.isNotEmpty()
                     saveContactButton.setTextColor(Color.parseColor("#CDCDCD"));
                 } else { // disabled
                     saveContactButton.isEnabled = false
                     saveContactButton.setTextColor(Color.parseColor("#878484"));
                 }
             } else {
                 saveContactButton.isEnabled = false
                 saveContactButton.setTextColor(Color.parseColor("#878484"));
             }*/
        }

        editContactPhoneNumberText.addTextChangedListener { editable ->
            enableDisableSave()
            /*  if (editable != null) {
                  if (editable.isNotEmpty() && editable.toString() != originalPhoneNumber) {
                      saveContactButton.isEnabled =
                          editContactNameText.text != null && editContactNameText.text.isNotEmpty()
                      saveContactButton.setTextColor(Color.parseColor("#CDCDCD"));
                  } else { // disabled
                      saveContactButton.isEnabled = false
                      saveContactButton.setTextColor(Color.parseColor("#878484"));
                  }
              } else {
                  saveContactButton.isEnabled = false
                  saveContactButton.setTextColor(Color.parseColor("#878484"));
              }*/
        }

        val phoneNumberToInsert = arguments?.getString("phone_number")
        if (phoneNumberToInsert != null) {
            originalPhoneNumber = phoneNumberToInsert
            editContactPhoneNumberText.setText(phoneNumberToInsert)
            val strContactId = getContactIdFromPhoneNumber(root.context, phoneNumberToInsert)
            contactId = strContactId?.toLong()
        }

        // User photo:
        initContactPhoto(root)

        // Is Contact in Favourites
        isContactInFav = phoneNumberToInsert?.let { isNumberInFavorites(it, root.context) } ?: false
        originalIsContactInFav = isContactInFav
        if (isContactInFav) {
            addToFavText.text = "Remove from favorites"
            addToFavImage.setImageResource(android.R.drawable.star_big_on)
        }
        addToFavImage.setOnClickListener {
            toggleIsInFavourites(root)
        }
        addToFavText.setOnClickListener {
            toggleIsInFavourites(root)
        }

        var isEdit = false // for now only adding new contacts

        val parameterContactName = arguments?.getString("contact_name")
        if (parameterContactName != null && parameterContactName !== "") {
            originalContactName = parameterContactName
            editContactNameText.setText(parameterContactName)
            isEdit = true
        } else { // New contact
            originalContactName = ""
        }
        saveContactButton.isEnabled = false
        saveContactButton.setTextColor(Color.parseColor("#878484"));

        val closeEditContactWindow = root.findViewById<TextView>(R.id.txtCancelButton)

        saveContactButton.setOnClickListener {
            val canReadContacts =
                (PermissionsStatus.readContactsPermissionGranted.value !== null && PermissionsStatus.readContactsPermissionGranted.value!!)

            if (canReadContacts) {
                if (contactIsValid(
                        isEdit,
                        root
                    )
                ) { // it also gives a toast so we don't need to - here
                    val canWriteContacts =
                        (PermissionsStatus.writeContactsPermissionGranted.value !== null && PermissionsStatus.writeContactsPermissionGranted.value!!)
                    if (canWriteContacts) { // we can go ahead and save the Contact
                        val wasSomethingSavedSuccessfully = saveContact(root, isEdit)
                        if (wasSomethingSavedSuccessfully) {
                            val sharedViewModel: SingleCallHistoryViewModel by activityViewModels()
                            sharedViewModel.setSharedData(
                                SingleCallHistoryViewModel.ContactData(
                                    editContactPhoneNumberText.text.toString(),
                                    editContactNameText.text.toString()
                                )
                            )
                            closeForm()
                        }
                    } else {
                        loadPermissionAlert(true)
                    }
                }
            } else {
                // Ask for permissions through Settings:
                loadPermissionAlert(false)
            }
        }

        closeEditContactWindow.setOnClickListener {
            closeForm()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*        val lastShownPremiumBirthdayReminderWasShowed = loadFromMemory(view.context,"premium_birthday_reminder_last_shown_date")
        val currDate = LocalDate.now()
        if (lastShownPremiumBirthdayReminderWasShowed == null || (getDaysDifference(lastShownPremiumBirthdayReminderWasShowed, currDate) > 3)) {
            saveInMemory(view.context,"premium_birthday_reminder_last_shown_date")
            val premiumVersionText = "Get the Premium version for a sophisticated Birthday Reminder!"
            Snackbar.make(view, premiumVersionText, Snackbar.LENGTH_LONG)
                .show()
        }*/
    }

    private fun getDaysDifference(firstDate: LocalDate, otherDate: LocalDate): Long {
        return ChronoUnit.DAYS.between(firstDate, otherDate)
    }

    // Function to save in SharedPreferences
    private fun saveInMemory(context: Context, variableToSave: String) {
        // Obtain SharedPreferences instance
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("SimpleCallPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save the phone number with a key
        editor.putString(variableToSave,  LocalDate.now().toString())
        editor.apply() // Apply changes asynchronously
    }

    // Function to save in SharedPreferences
    private fun loadFromMemory(context: Context, variableToLoad: String): LocalDate? {
        val sharedPreferences =
            context.getSharedPreferences("SimpleCallPreferences", Context.MODE_PRIVATE)

        val dateString = sharedPreferences?.getString(variableToLoad, null)

        return dateString?.let {
            try {
                LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: DateTimeParseException) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun toggleIsInFavourites(root: View) {
        val addToFavText = root.findViewById<TextView>(R.id.addToFavText)
        val addToFavImage = root.findViewById<ImageView>(R.id.addToFavImage)

        isContactInFav = !isContactInFav
        if (isContactInFav) {
            addToFavText.text = "Remove from favorites"
            addToFavImage.setImageResource(android.R.drawable.star_big_on)
        } else {
            addToFavImage.setImageResource(R.drawable.favstartransparent)
            addToFavText.text = "Add to favorites"
        }
        enableDisableSave()
    }

    private fun enableDisableSave() {
        val root: View = binding.root

        val editContactNameText: EditText = root.findViewById(R.id.editContactName)
        val editContactPhoneNumberText: EditText =
            root.findViewById(R.id.editContactPhoneNumberText)
        val saveContactButton = root.findViewById<TextView>(R.id.txtSaveButton)
        var shouldEnable = false

        shouldEnable = editContactNameText.text != null && editContactNameText.text.isNotEmpty() &&
                editContactPhoneNumberText.text != null && editContactPhoneNumberText.text.isNotEmpty()

        if (shouldEnable) {
            val isUserPhotoDifferent =
                (::userImage.isInitialized) && ((originalUserImage == null) || (userImage != originalUserImage))

            val isDifferent = editContactNameText.text.toString() != originalContactName ||
                    editContactPhoneNumberText.text.toString() != originalPhoneNumber
                    || isUserPhotoDifferent
                    || (isContactInFav != originalIsContactInFav)

            if (isDifferent) {
                saveContactButton.isEnabled = true
                saveContactButton.setTextColor(Color.parseColor("#CDCDCD"));
            } else { // disabled
                saveContactButton.isEnabled = false
                saveContactButton.setTextColor(Color.parseColor("#878484"));
            }
        } else { // disabled
            saveContactButton.isEnabled = false
            saveContactButton.setTextColor(Color.parseColor("#878484"));
        }
    }

    private fun closeForm() {
        exitTransition = android.transition.Fade() // Add fade effect
        parentFragmentManager.popBackStack()  // This will remove the current fragment and return to the previous one
    }

    private fun loadPermissionAlert(isWrite: Boolean) {
        // Ask for permissions through Settings:
        val overlayFragment = PermissionsAlertFragment()
        val args = Bundle().apply {
            putBoolean("IS_READ_CONTACTS_PERMISSION", !isWrite)
            putBoolean("IS_WRITE_CONTACTS_PERMISSION", isWrite)
        }
        overlayFragment.arguments = args
        overlayFragment.show(parentFragmentManager, "PermissionMissingAlertDialogTag")
    }

    // canSaveContact - return -1 if no read contacts, -2 if no write contacts and 0 if all is good
    private fun contactIsValid(isEdit: Boolean, rootView: View): Boolean {
        val editContactNameText: EditText = rootView.findViewById(R.id.editContactName)
        val contactName = editContactNameText.text.toString()
        var isValid = false

        if (contactName.isNotEmpty()) {
            if (PermissionsStatus.readContactsPermissionGranted.value !== null && PermissionsStatus.readContactsPermissionGranted.value!!) {
                val contactsMap = getContacts(rootView.context)

                // Check if Contact to save already exists:
                val existingContact = contactsMap[contactName]
                if (!isEdit && existingContact != null) {
                    // Can't add this contact because it already exists
                    Toast.makeText(
                        context,
                        "Cannot save new Contact. Contact already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                    isValid = false
                } else if (isEdit && (contactName != originalContactName && existingContact != null)) {
                    Toast.makeText(
                        context,
                        "Cannot edit Contact name. Contact with this name already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                    isValid = false
                } else {
                    // We have Read Contacts permission and the Contact is ok.
                    // Return if we have Write Contacts permission to save the Contact
                    //if (PermissionsStatus.writeContactsPermissionGranted.value !== null && PermissionsStatus.writeContactsPermissionGranted.value!!) {
                    isValid =
                        true // we already checked if Contact name is not empty and if we have Read Contacts permission
                    //} else { // We need Write Contacts permission
                }
            }
        }

        return isValid
    }

    fun setContactAsFavorite(context: Context, contactId: Long, isFavorite: Boolean): Boolean {
        val uri = ContactsContract.Contacts.CONTENT_URI
        val values = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
        }

        val selection = "${ContactsContract.Contacts._ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        val rowsUpdated = context.contentResolver.update(
            uri,
            values,
            selection,
            selectionArgs
        )

        return rowsUpdated > 0 // Return true if the operation succeeded
    }


    // Function to save phone number in SharedPreferences
    private fun saveContact(root: View, isEdit: Boolean): Boolean {
        var wasSomethingSaved = false
        var newContactId: Long? = null

        if (contactId !== null || !isEdit) {
            val editContactNameText: EditText = root.findViewById(R.id.editContactName)
            val editContactPhoneNumberText: EditText =
                root.findViewById(R.id.editContactPhoneNumberText)
            // val editContactBirthdayText = root.findViewById<EditText>(R.id.editContactBirthday)
            // val editContactBirthdayReminderChip = root.findViewById<Chip>(R.id.editContactBirthdayReminderChip)
            // val closeEditContactWindow = root.findViewById<ImageView>(R.id.closeEditContactWindow)
            val resultsContactId = saveOrUpdateContact(
                root.context,
                contactId,
                editContactNameText.text.toString(),
                editContactPhoneNumberText.text.toString(),
            )

            if (resultsContactId != null) {
                if (contactId == null) {
                    contactId = resultsContactId // this is from now the Contact ID
                }
                wasSomethingSaved = true
            }


            // Picture:
            if (::userImage.isInitialized) {
                if ((originalUserImage == null) || (userImage != originalUserImage)) {
                    val wasSaved = saveOrUpdateContactPhoto(root.context, userImage, contactId!!)
                    if (wasSaved && !wasSomethingSaved) {
                        wasSomethingSaved = true
                    }
                }
            }

            // Favorite:
            if (isContactInFav != originalIsContactInFav) {
                // Save fav
                val wasSaved = setContactAsFavorite(root.context, contactId!!, isContactInFav)
                if (wasSaved && !wasSomethingSaved) {
                    wasSomethingSaved = true
                }
            }
        }
        return wasSomethingSaved
    }

    private fun updateExistingContactNameAndPhone(
        context: Context,
        contactId: Long,
        newName: String,
        newPhoneNumber: String
    ): Boolean {
        if (newName.isEmpty() || (newName == originalContactName && newPhoneNumber == originalPhoneNumber)) {
            return false // nothing new to save
        }

        val contentResolver = context.contentResolver
        val operations = ArrayList<ContentProviderOperation>()

        // Update contact name
        if (newName != originalContactName) {
            operations.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(
                            contactId.toString(),
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                        )
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, newName)
                    .build()
            )
        }

        if (newPhoneNumber != originalPhoneNumber) {
            // Update contact phone number
            operations.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(
                            contactId.toString(),
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhoneNumber)
                    .build()
            )
        }



        return try {
            // Apply batch update
            contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveOrUpdateContact(
        context: Context,
        existingContactId: Long?,
        newName: String,
        newPhoneNumber: String
    ): Long? {
        if (newName.isEmpty() || newPhoneNumber.isEmpty()) {
            return null // שם ומספר טלפון חייבים להיות מסופקים
        }

        val contentResolver = context.contentResolver
        val operations = ArrayList<ContentProviderOperation>()

        if (existingContactId != null) { // עדכון איש קשר קיים
            // כאן עליך לוודא שיש לך את המשתנים originalContactName ו-originalPhoneNumber
            val isNameDifferent = newName != originalContactName
            val isPhoneDifferent = newPhoneNumber != originalPhoneNumber

            if (!isNameDifferent && !isPhoneDifferent) {
                return null // אין מה לעדכן
            }

            if (isNameDifferent) {
                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                existingContactId.toString(),
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                            newName
                        )
                        .build()
                )
            }

            // עדכון מספר הטלפון
            if (isPhoneDifferent) {
                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                existingContactId.toString(),
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhoneNumber)
                        .build()
                )
            }
        } else { // יצירת איש קשר חדש
            val rawContactInsertIndex = operations.size // אינדקס של RawContact החדש

            // הוספת RawContact חדש
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // הוספת שם לאיש הקשר החדש
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(
                        ContactsContract.Data.RAW_CONTACT_ID,
                        rawContactInsertIndex
                    )
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, newName)
                    .build()
            )

            // הוספת מספר טלפון לאיש הקשר החדש
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(
                        ContactsContract.Data.RAW_CONTACT_ID,
                        rawContactInsertIndex
                    )
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhoneNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )
        }

        return try {
            // ביצוע פעולות ה-batch
            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)

            if (existingContactId == null) { // יצירת איש קשר חדש
                // שליפת RawContact ID מהתוצאה הראשונה
                val rawContactUri = results[0].uri
                val rawContactId = rawContactUri?.lastPathSegment?.toLongOrNull()

                if (rawContactId != null) {
                    // שאילתה כדי לקבל את Contact ID מתוך RawContact ID
                    val contactIdProjection = arrayOf(ContactsContract.RawContacts.CONTACT_ID)
                    val selection = "${ContactsContract.RawContacts._ID} = ?"
                    val selectionArgs = arrayOf(rawContactId.toString())

                    val cursor = contentResolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        contactIdProjection,
                        selection,
                        selectionArgs,
                        null
                    )

                    var newContactId: Long? = null
                    cursor?.use {
                        if (it.moveToFirst()) {
                            newContactId =
                                it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID))
                        }
                    }

                    newContactId // החזרת Contact ID החדש
                } else {
                    null // כישלון בשליפת RawContact ID
                }
            } else {
                existingContactId // החזרת Contact ID הקיים
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SavePhotoToContact", "Failed to save or update Contact with contact id ($existingContactId)", e)
            null // כישלון בביצוע הפעולה
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        val isPremium = resources.getBoolean(R.bool.isAdvancedDialer)
        if (isPremium) {
            // show it again
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            fab?.visibility = View.VISIBLE
            _binding = null
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                askingReadContactsPermission = false
                PermissionsStatus.readContactsPermissionGranted.value = true
                Toast.makeText(
                    context,
                    "Read Contacts permission was granted! Please try again",
                    Toast.LENGTH_SHORT
                ).show()
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
        } else if (PermissionsStatus.writeContactsPermissionGranted.value === null || (!(PermissionsStatus.writeContactsPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    WRITE_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                askingWriteContactsPermission = false
                PermissionsStatus.writeContactsPermissionGranted.value = true
                Toast.makeText(
                    context,
                    "Write Contacts permission was granted! Please try again",
                    Toast.LENGTH_SHORT
                ).show()
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
        }
    }

    private fun getContacts(context: Context): MutableMap<String, String> {
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

        val contactsMap = mutableMapOf<String, String>()
        cursor?.use {
            while (it.moveToNext()) {
                val name =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phone =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                contactsMap[name] = phone
            }
        }

        return contactsMap
    }

    // Convert Uri to Bitmap
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (uri.path?.contains("/raw/") == true) {
                getBitmapFromMiuiUri(uri)
            } else {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getContactIdFromPhoneNumber(context: Context, phoneNumber: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val contactIdIndex =
                        cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    return cursor.getString(contactIdIndex)
                }
            }
        return null
    }

    private fun getBitmapFromMiuiUri(uri: Uri): Bitmap? {
        val uriPath = uri.path ?: return null
        if (uriPath.contains("/raw/")) {
            val rawPath = uriPath.substringAfter("/raw/")
            return BitmapFactory.decodeFile(rawPath)
        }
        return null
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        requireContext().contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    // Save the Bitmap as contact photo
    private fun saveOrUpdateContactPhoto(
        context: Context,
        bitmap: Bitmap,
        contactId: Long
    ): Boolean {
        val rawContactId =
            getRawContactId(context, contactId) ?: return false // ודא שהשגת את RAW_CONTACT_ID

        return try {
            val photoBytes = getPhotoBytes(bitmap)

            // בדוק אם קיימת תמונה קיימת
            val uri = ContactsContract.Data.CONTENT_URI
            val selection =
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(
                rawContactId.toString(),
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
            )

            val cursor = context.contentResolver.query(uri, null, selection, selectionArgs, null)
            val hasExistingPhoto = cursor?.use { it.moveToFirst() } == true

            if (hasExistingPhoto) {
                // אם כבר קיימת תמונה, עדכן אותה
                val contentValues = ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                }
                val rowsUpdated =
                    context.contentResolver.update(uri, contentValues, selection, selectionArgs)
                rowsUpdated > 0
            } else {
                // אחרת, הוסף תמונה חדשה
                val contentValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                    )
                    put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                }
                val insertedUri = context.contentResolver.insert(uri, contentValues)
                insertedUri != null
            }
        } catch (e: Exception) {
            Log.e("SavePhotoToContact", "Failed to save or update photo", e)
            false
        }
    }

    private fun getRawContactId(context: Context, contactId: Long): Long? {
        val uri = ContactsContract.RawContacts.CONTENT_URI
        val projection = arrayOf(ContactsContract.RawContacts._ID)
        val selection = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                }
            }
        return null
    }

    // Convert Bitmap to byte array
    private fun getPhotoBytes(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    // Function to launch the image picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun initContactPhoto(root: View) {
        addContactPhoto = root.findViewById(R.id.addContactPhoto)
        addContactPhoto.setOnClickListener {
            openImagePicker()
        }

        contactExistingPhotoBack = root.findViewById(R.id.contactExistingPhotoBack)
        contactExistingPhotoBack.setOnClickListener {
            openImagePicker()
        }
        contactExistingPhoto = root.findViewById(R.id.contactExistingPhoto)
        contactExistingPhoto.setOnClickListener {
            openImagePicker()
        }
        textAddChangePhoto.setOnClickListener {
            openImagePicker()
        }

        if (contactId != null) {
            val userProfilePicture = getContactPhoto(root.context, contactId.toString())
            val contactExistingPhoto = root.findViewById<ImageView>(R.id.contactExistingPhoto)
            if (userProfilePicture != null) {
                contactExistingPhoto.setImageBitmap(userProfilePicture)
                contactExistingPhotoBack.visibility = VISIBLE
                addContactPhoto.visibility = GONE
                textAddChangePhoto.text = "Change photo"
                val scale = resources.displayMetrics.density
                val dpWidth = (120 * scale + 0.5f).toInt() // converts 200dp to pixels
                val params = textAddChangePhoto.layoutParams
                params.width = dpWidth
                textAddChangePhoto.layoutParams = params
            } else {
                // Handle case where no profile picture exists
                contactExistingPhotoBack.visibility = GONE
                addContactPhoto.visibility = VISIBLE
                textAddChangePhoto.text = "Add photo"
                val scale = resources.displayMetrics.density
                val dpWidth = (88 * scale + 0.5f).toInt() // converts 200dp to pixels
                textAddChangePhoto.width = dpWidth
            }
        }
    }

    private fun fetchAndDisplayContactPhoto(contactId: Long) {
        val photoUri = ContactsContract.Data.CONTENT_URI
        val resolver = requireContext().contentResolver

        // Query to get the photo data
        val cursor = resolver.query(
            photoUri,
            arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
            null
        )

        // If the photo exists, decode it into a Bitmap and display it
        cursor?.use {
            if (it.moveToFirst()) {
                val photoBytes =
                    it.getBlob(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Photo.PHOTO))
                if (photoBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                    addContactPhoto.visibility = GONE
                    contactExistingPhoto.setImageBitmap(bitmap)
                    contactExistingPhotoBack.visibility = VISIBLE
                    textAddChangePhoto.text = "Change photo"
                    val scale = resources.displayMetrics.density
                    val dpWidth = (120 * scale + 0.5f).toInt() // converts 200dp to pixels
                    textAddChangePhoto.width = dpWidth
                } else {
                    // Set a placeholder image if no photo is found
                    addContactPhoto.visibility = VISIBLE
                    contactExistingPhotoBack.visibility = GONE
                    textAddChangePhoto.text = "Add photo"
                    val scale = resources.displayMetrics.density
                    val dpWidth = (88 * scale + 0.5f).toInt() // converts 200dp to pixels
                    textAddChangePhoto.width = dpWidth
                }
            } else {
                // Set a placeholder image if no photo is found
                addContactPhoto.setImageResource(R.drawable.camerawithcircletransparent)
                addContactPhoto.visibility = VISIBLE
                contactExistingPhotoBack.visibility = GONE
                textAddChangePhoto.text = "Add photo"
                val scale = resources.displayMetrics.density
                val dpWidth = (88 * scale + 0.5f).toInt() // converts 200dp to pixels
                textAddChangePhoto.width = dpWidth
            }
        } ?: run {
            // Handle error or no contact photo found
            //addContactPhoto.setImageResource(R.drawable.camerawithcircletransparent)
            addContactPhoto.visibility = VISIBLE
            contactExistingPhotoBack.visibility = GONE
            textAddChangePhoto.text = "Add photo"
            val scale = resources.displayMetrics.density
            val dpWidth = (88 * scale + 0.5f).toInt() // converts 200dp to pixels
            textAddChangePhoto.width = dpWidth
        }
    }
}