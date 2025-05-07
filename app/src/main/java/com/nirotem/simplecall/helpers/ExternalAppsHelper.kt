package com.nirotem.simplecall.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.NumberParseException
import com.nirotem.simplecall.R

object ExternalAppsHelper {
    fun openWhatsAppContactThatHaveIssueWhenNoCountryKidomet(phoneNumber: String, context: Context) {
        // פורמט המספר הבינלאומי ללא סימני פלוס או רווחים
        val formattedNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")

        // URI לפורמט wa.me
        val uri = "https://wa.me/$formattedNumber"

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(uri)

        // בדיקה אם יש אפליקציה שיכולה לטפל באינטנט
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // טיפול במקרה שאין WhatsApp מותקן
            Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_LONG).show()
        }
    }

    // Add this dependency in your build.gradle (module) file:
// implementation 'com.googlecode.libphonenumber:libphonenumber:8.12.57'

/*    import com.google.i18n.phonenumbers.PhoneNumberUtil
    import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
    import com.google.i18n.phonenumbers.NumberParseException*/

    fun openWhatsAppContact(phoneNumber: String, context: Context) {
        val phoneUtil = PhoneNumberUtil.getInstance()
        try {
            // Use the device's default region (ISO country code) as a hint.
            val regionCode = Locale.getDefault().country  // e.g., "US", "IL", "GB", etc.

            // Parse the phone number. If the number is not in international format,
            // the region code will help to infer the correct country code.
            val numberProto = phoneUtil.parse(phoneNumber, regionCode)

            // Format the number in E164 format (e.g., +1234567890).
            // Remove the leading "+" because the wa.me URL expects just the digits.
            val formattedNumber =
                phoneUtil.format(numberProto, PhoneNumberFormat.E164).replace("+", "")

            // Construct the URL for WhatsApp
            val uri = "https://wa.me/$formattedNumber"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

            context.startActivity(intent)
        } catch (e: NumberParseException) {
            Toast.makeText(context,
                context.getString(R.string.invalid_phone_number_capital), Toast.LENGTH_LONG).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context,
                context.getString(R.string.whatsapp_is_not_installed_capital), Toast.LENGTH_LONG).show()
        }
    }


    fun openSMSApp(phoneNumber: String, context: Context) {
        // Create the URI with the phone number
        val uri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_SENDTO, uri)

        // Verify that there is an app to handle the intent
        // בדיקה אם יש אפליקציה שיכולה לטפל באינטנט
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // טיפול במקרה שאין WhatsApp מותקן
            Toast.makeText(context, "No SMS app found on your device.", Toast.LENGTH_LONG).show()
        }

        /*   if (intent.resolveActivity(context.packageManager) != null) {
               context.startActivity(intent)
           } else {
               // Handle the case where no SMS app is available
               Toast.makeText(this, "No SMS app found on your device.", Toast.LENGTH_SHORT).show()
           }*/
    }

}