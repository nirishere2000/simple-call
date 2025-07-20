package com.nirotem.simplecall.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nirotem.simplecall.R
import com.nirotem.simplecall.statuses.AllowOutgoingCallsEnum
import com.nirotem.simplecall.statuses.SettingsStatus
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SharedPreferencesCache {
    const val EASY_CALL_AND_ANSWER_SHARED_FILE = "SimpleCallAppSharePreferences"

    // Function to save phone number in SharedPreferences
     fun saveVariableInMemory(context: Context, varToSave: String, valToSave: String?) {
        // Obtain SharedPreferences instance
        val sharedPrefApp = EASY_CALL_AND_ANSWER_SHARED_FILE // if (sharedPrefApp != null) sharedPrefApp else "SimpleCallPreferences"
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(sharedPrefApp, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save the phone number with a key
        editor.putString(varToSave, valToSave)
        editor.apply() // Apply changes asynchronously
    }

    // Function to save in SharedPreferences
    fun loadVariableFromMemory(variableToLoad: String, context: Context?): String? {
        try {
            if (context != null) {
                val sharedPrefApp = EASY_CALL_AND_ANSWER_SHARED_FILE // if (sharedPrefApp != null) sharedPrefApp else "SimpleCallPreferences"
                val sharedPreferences: SharedPreferences = context.getSharedPreferences(
                    sharedPrefApp,
                    Context.MODE_PRIVATE
                )
                return sharedPreferences.getString(variableToLoad, null)
            }
        }
        catch (e: Exception) {
            Log.e("Simple Call - SharedPreferencesCache", "Error on loadVariableFromMemory ($e)")
        }
        return null
    }

    fun saveAllAutoAnswersAsFalse(context: Context) { // for reset
        val sharedPreferences = context.getSharedPreferences("SimpleCallPreferences", Context.MODE_PRIVATE)
        // מסננים את כל המפתחות שמסתיימים ב־"_aa"
        sharedPreferences.all.filterKeys { it.endsWith("_aa_aa") } // aa = auto answer
            .forEach { (key, value) ->
                // עבור כל מפתח, קוראים לפונקציה עם המפתח כערך
                saveAutoAnswer(key, false, context)
            }
    }

    fun saveAutoAnswer(phoneNumber: String?, isChecked: Boolean, context: Context) {
        if (phoneNumber != null) {
            val autoAnswerKey = "${phoneNumber}_aa_aa" // aa = auto answer
            val valToSave = if (isChecked) "true" else "false"
            saveVariableInMemory(context, autoAnswerKey, valToSave)
        }
    }

    fun loadGoldNumber(context: Context?): String? {
        return loadVariableFromMemory("gold_phone_number", context)
    }

    fun loadGoldNumberContact(context: Context?): String? {
        return loadVariableFromMemory("gold_phone_contactName", context)
    }

    fun saveGoldNumber(goldPhoneNumber: String?, context: Context) {
        SettingsStatus.goldNumber.value = goldPhoneNumber
        saveVariableInMemory(context, "gold_phone_number", goldPhoneNumber)
    }

    fun saveGoldNumberContact(goldPhoneNumberContact: String?, context: Context) {
        SettingsStatus.goldNumberContact.value = goldPhoneNumberContact
        saveVariableInMemory(context, "gold_phone_contactName", goldPhoneNumberContact)
    }



  /*  fun loadCallsReportIsGoldNumber(context: Context): Boolean {
        var callsReportIsGoldNumber = loadCallsReportIsGoldNumberLib(context)

        if (callsReportIsGoldNumber.isNullOrEmpty()) {
            return false // we can't start with calls report sending on (in case it's gold number it will be on)
        }
        else {
            return callsReportIsGoldNumber == "true"
        }
    }

    fun saveCallsReportNumber(callsReportPhoneNumber: String?, context: Context) {
        SettingsStatus.callsReportPhoneNumber.value = callsReportPhoneNumber
        saveCallsReportNumberLib(callsReportPhoneNumber, context)
    }

    fun saveCallsReportContact(callsReportContact: String?, context: Context) {
        SettingsStatus.callsReportContact.value = callsReportContact
        saveCallsReportContactLib("calls_report_contactName", context)
    }

    fun saveCallsReportIsGoldNumber(callsReportIsGoldNumber: Boolean, context: Context) {
        SettingsStatus.callsReportIsGoldNumber.value = callsReportIsGoldNumber
        saveCallsReportIsGoldNumberLib(callsReportIsGoldNumber, context)
    }*/

    fun shouldAllowOpeningWhatsApp(context: Context): Boolean {
        var allowOpeningWhatsApp = loadVariableFromMemory("AllowOpeningWhatsApp", context)

        if (allowOpeningWhatsApp.isNullOrEmpty()) {
            return context.resources.getBoolean(R.bool.allowOpenWhatsApp)
        }
        else {
            return allowOpeningWhatsApp == "true"
        }
    }

    fun saveAllowOpeningWhatsApp(allowOpeningWhatsApp: Boolean, context: Context) {
        SettingsStatus.allowOpeningWhatsApp.value = allowOpeningWhatsApp
        val valToSave = if (allowOpeningWhatsApp) "true" else "false"
        saveVariableInMemory(context, "AllowOpeningWhatsApp", valToSave)
    }

    fun saveShouldCallsStartWithSpeakerOn(shouldStartWithSpeakerOn: Boolean, context: Context) {
        val valToSave = if (shouldStartWithSpeakerOn) "true" else "false"
        saveVariableInMemory(context, "StartCallWithSpeakerOn", valToSave)
    }

    fun shouldCallsStartWithSpeakerOn(context: Context): Boolean {
        var shouldStartWithSpeakerOn = loadVariableFromMemory("StartCallWithSpeakerOn", context)

        if (shouldStartWithSpeakerOn.isNullOrEmpty()) {
            return context.resources.getBoolean(R.bool.startCallWithSpeakerOn)
        }
        else {
            return shouldStartWithSpeakerOn == "true"
        }
    }

    fun saveAllowCallWaiting(shouldAllowCallWaiting: Boolean, context: Context) {
        val valToSave = if (shouldAllowCallWaiting) "true" else "false"
        saveVariableInMemory(context, "ShouldAllowCallWaiting", valToSave)
    }

    fun shouldAllowCallWaiting(context: Context): Boolean {
        var shouldAllowCallWaiting = loadVariableFromMemory("ShouldAllowCallWaiting", context)

        if (shouldAllowCallWaiting.isNullOrEmpty()) {
            return context.resources.getBoolean(R.bool.shouldAllowCallWaiting)
        }
        else {
            return shouldAllowCallWaiting == "true"
        }
    }

    fun shouldShowKeypadInActiveCall(context: Context?): Boolean {
        val shouldShowKeypadInActiveCall = loadVariableFromMemory("ShouldShowKeypadInActiveCall", context)
        if (shouldShowKeypadInActiveCall.isNullOrEmpty()) {
            val resourceShouldShowKeypadInActiveCall = context?.resources?.getBoolean(R.bool.shouldShowKeypadInActiveCall)
            return resourceShouldShowKeypadInActiveCall != null && resourceShouldShowKeypadInActiveCall == true
        }
        return shouldShowKeypadInActiveCall == "true"
    }

    fun saveShouldShowKeypadInActiveCall(shouldShowKeypadInActiveCall: Boolean, context: Context) {
        val valToSave = if (shouldShowKeypadInActiveCall) "true" else "false"
        saveVariableInMemory(context, "ShouldShowKeypadInActiveCall", valToSave)
    }

    fun saveIsGlobalAutoAnswer(isGlobalAutoAnswer: Boolean, context: Context) {
        val valToSave = if (isGlobalAutoAnswer) "true" else "false"
        saveVariableInMemory(context, "IsAutoAnswer", valToSave)
    }

    fun isGlobalAutoAnswer(context: Context?): Boolean {
        val isAutoAnswer = loadVariableFromMemory("IsAutoAnswer", context)
        if (isAutoAnswer.isNullOrEmpty()) {
            val resourceAutoAnswer = context?.resources?.getBoolean(R.bool.autoAnswerAllCalls)
            return resourceAutoAnswer != null && resourceAutoAnswer == true
        }
        return isAutoAnswer == "true"
    }

    fun shouldAutoAnswerPhoneNumber(phoneNumber: String, context: Context?): Boolean {
        val autoAnswerKey = "$phoneNumber}_aa" // aa = auto answer
        val isAutoAnswerVar = loadVariableFromMemory(autoAnswerKey, context)
        return isAutoAnswerVar == "true"
    }

    fun saveIsInMiddleOfCallIsOutgoing(context: Context, isInMiddleIsOutgoing: Boolean) {
        val valToSave = if (isInMiddleIsOutgoing) "true" else "false"
        saveVariableInMemory(context, "isInMiddleOfCallIsOutgoing", valToSave)
    }

    fun saveIsInMiddleOfCallIsCalling(context: Context, isInMiddleIsCalling: Boolean) {
        val valToSave = if (isInMiddleIsCalling) "true" else "false"
        saveVariableInMemory(context, "isInMiddleOfCallIsCalling", valToSave)
    }

    fun isInMiddleOfCallIsOutgoing(context: Context): Boolean {
        val isInMiddleOfCallIsOutgoingStr = loadVariableFromMemory("isInMiddleOfCallIsOutgoing", context)
        return isInMiddleOfCallIsOutgoingStr == "true"
    }

    fun isInMiddleOfCallIsCalling(context: Context): Boolean {
        val isInMiddleOfCallIsOutgoingStr = loadVariableFromMemory("isInMiddleOfCallIsCalling", context)
        return isInMiddleOfCallIsOutgoingStr == "true"
    }

    fun loadAllowAnswerCallsEnum(context: Context): String? {
        val allowAnswerCallsMode = loadVariableFromMemory("allowAnswerCallsMode", context)
        if (allowAnswerCallsMode.isNullOrEmpty()) {
            val resourceAllowAnswerCallsMode = context.resources?.getString(R.string.allowAnswerCallsMode)
            return resourceAllowAnswerCallsMode
        }
        return allowAnswerCallsMode
    }

    fun saveAllowAnswerCallsEnum(context: Context, allowAnswerCallsEnumString: String) {
        saveVariableInMemory(context, "allowAnswerCallsMode", allowAnswerCallsEnumString)
    }

    fun loadAllowMakingCallsEnum(context: Context): String? {
        val allowOutgoingCallsMode = loadVariableFromMemory("allowOutgoingCallsMode", context)
        if (allowOutgoingCallsMode.isNullOrEmpty()) {
            val resourceAllowOutgoingCallsMode = context.resources?.getString(R.string.allowOutgoingCallsMode)
            return resourceAllowOutgoingCallsMode
        }
        return allowOutgoingCallsMode
    }

    fun saveAllowMakingCallsEnum(context: Context, allowOutgoingCallsEnumString: String) {
        SettingsStatus.userAllowOutgoingCallsEnum.value = AllowOutgoingCallsEnum.valueOf(allowOutgoingCallsEnumString)
        saveVariableInMemory(context, "allowOutgoingCallsMode", allowOutgoingCallsEnumString)
    }

    fun loadNumOfTimesWarnedForShowBattery(context: Context): Int {
        val numOfTimesWarnedForShowBatteryStr = loadVariableFromMemory("numOfTimesWarnedForShowBattery", context)
        if (numOfTimesWarnedForShowBatteryStr.isNullOrEmpty()) {
            return 0
        }
        return numOfTimesWarnedForShowBatteryStr.toInt()
    }

    fun saveNumOfTimesWarnedForShowBattery(context: Context, numOfTimesWarnedForShowBattery: Int) {
        saveVariableInMemory(context, "numOfTimesWarnedForShowBattery", numOfTimesWarnedForShowBattery.toString())
    }

    // Tour
    fun setTourShown(context: Context, reset: Boolean = false) {
        if (!reset) {
            saveVariableInMemory(context, "tourAlreadyShown", "true")
        }
        else { // delete
            saveVariableInMemory(context, "tourAlreadyShown", null)
        }
    }

    fun wasTourAlreadyShown(context: Context): Boolean {
        val isTourAlreadyShown = loadVariableFromMemory("tourAlreadyShown", context)
        return isTourAlreadyShown == "true"
    }

    fun saveUpdatedTourCaption(updatedCaption: String?, context: Context) {
        saveVariableInMemory(context, "updatedTourCaption", updatedCaption)
    }

    fun loadUpdatedTourCaption(context: Context): String? {
        return loadVariableFromMemory("updatedTourCaption", context)
    }

    // null = not init,
    // "" (empty) = tried to make phone call,
    // now = success loading activity when trying to make a phone call
    fun loadCallActivityLoadedTimeStamp(context: Context): String? {
        val timeStampOrEmptyOrNull = loadVariableFromMemory("activityLoadedTimeStamp", context)
        return timeStampOrEmptyOrNull
    }

    fun saveCallActivityLoadedTimeStamp(context: Context, reset: Boolean = false) {
        val now = System.currentTimeMillis().toString()
        val varToSave = if (reset) "" else now // null = not init,
        // "" (empty) = tried to make phone call,
        // now = success loading activity when trying to make a phone call
        saveVariableInMemory(context, "activityLoadedTimeStamp", varToSave)
    }

    fun loadIsAppLoaded(context: Context): Boolean {
        val isAppLoaded = loadVariableFromMemory("isAppLoaded", context)
        return isAppLoaded == "true"
    }

    fun saveIsAppLoaded(context: Context, isAppLoaded: Boolean) {
        val varToSave = if (isAppLoaded) "true" else "false"
        saveVariableInMemory(context, "isAppLoaded", varToSave)
    }

    fun loadLastTimeAnimateGoldNumberInContacts(context: Context): String? {
        val timeStampOrEmptyOrNull = loadVariableFromMemory("LastTimeAnimateGoldNumberInContacts", context)
        return timeStampOrEmptyOrNull
    }

    fun saveLastTimeAnimateGoldNumberInContacts(context: Context, reset: Boolean = false) {
        val now = System.currentTimeMillis().toString()
        val varToSave = if (reset) "" else now // null = not init,
        // "" (empty) = tried to make phone call,
        // now = success loading activity when trying to make a phone call
        saveVariableInMemory(context, "LastTimeAnimateGoldNumberInContacts", varToSave)
    }

    // פונקציה לשמירת המיפוי (שם -> טלפון) ב-SharedPreferences בעזרת org.json
    fun saveContactsMapping(context: Context, contactsMap: Map<String, List<String>>) {
        val jsonObject = JSONObject(contactsMap)
        saveVariableInMemory(context, "contacts_mapping", jsonObject.toString())
    }

    // פונקציה לטעינת המיפוי
    fun getContactNameFromPhoneNumberInJson(context: Context, phoneNumber: String): String? {
        val jsonString = loadVariableFromMemory("contacts_mapping", context)

        if (jsonString != null) {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
                if (map[key] == phoneNumber) {
                    return key // key is Contact name
                }
            }
            return null
        }
        return null
    }

    // If we get external call request then we start from the activity, which should make the call
    // So we must save the time and seconds so we won't load the Active Call fragment again
    fun saveLastExternalCallDate(context: Context, dateToSave: String) {
        saveVariableInMemory(context, "LastTimeAnimateGoldNumberInContacts", dateToSave)
    }

    fun loadLastExternalCallDate(context: Context): String? {
        val lastExternalCallDate = loadVariableFromMemory("LastTimeAnimateGoldNumberInContacts", context)
        return lastExternalCallDate
    }

    // If call had fatal error, save it to persist memory so we can show it to user next time
    fun saveLastCallError(context: Context, errorMsgToSave: String?) {
        saveVariableInMemory(context, "LastCallErrorMessage", errorMsgToSave)
    }

    fun loadLastCallError(context: Context): String? {
        val lastCallError = loadVariableFromMemory("LastCallErrorMessage", context)
        return lastCallError
    }

    fun loadUserAlreadyOpenedTermsAndConditionsOnce(context: Context?): Boolean? {
        val userAlreadyOpenedTermsAndConditionsOnce = loadVariableFromMemory("user_opened_terms_and_conditions_screen_once", context)

        return userAlreadyOpenedTermsAndConditionsOnce == "true"
    }

    fun saveUserAlreadyOpenedTermsAndConditionsOnce(userAlreadyOpenedTermsAndConditionsOnce: Boolean, context: Context) {
        val valToSave = if (userAlreadyOpenedTermsAndConditionsOnce) "true" else "false"
        saveVariableInMemory(context, "user_opened_terms_and_conditions_screen_once", valToSave)
    }

    fun loadUserApprovedTermsAndConditions(context: Context?): Boolean? {
        val userApprovedTermsAndConditions = loadVariableFromMemory("user_approved_terms_and_conditions", context)

        return userApprovedTermsAndConditions == "true"
    }

    fun saveUserApprovedTermsAndConditions(userApprovedTermsAndConditions: Boolean, context: Context) {
        val valToSave = if (userApprovedTermsAndConditions) "true" else "false"
        saveVariableInMemory(context, "user_approved_terms_and_conditions", valToSave)
    }

    fun loadQuickCallNumber(context: Context?): String? {
        return loadVariableFromMemory("quick_call_phone_number", context)
    }

    fun saveQuickCallNumber(quickCallNumber: String?, context: Context) {
        SettingsStatus.quickCallNumber.value = quickCallNumber
        saveVariableInMemory(context, "quick_call_phone_number", quickCallNumber)
    }

    fun saveQuickCallNumberContact(quickCallNumberContact: String?, context: Context) {
        SettingsStatus.quickCallNumberContact.value = quickCallNumberContact
        saveVariableInMemory(context, "quick_call_phone_number_contact", quickCallNumberContact)
    }

    fun loadQuickCallNumberContact(context: Context?): String? {
        return loadVariableFromMemory("quick_call_phone_number_contact", context)
    }

    fun getAppVersionFromCache(context: Context?): String? {
        return loadVariableFromMemory("curr_app_version", context)
    }

    fun saveAppVersionInCache(appVersion: String?, context: Context) {
        saveVariableInMemory(context, "curr_app_version", appVersion)
    }

    fun getAnswerCallVoiceCommand(context: Context?): String? {
        return loadVariableFromMemory("answer_call_voice_command", context)
    }

    fun saveAnswerCallVoiceCommand(voiceCommand: String?, context: Context) {
        saveVariableInMemory(context, "answer_call_voice_command", voiceCommand)
    }

    fun getDistressButtonVoiceCommand(context: Context?): String? {
        return loadVariableFromMemory("distress_button_voice_command", context)
    }

    fun saveDistressButtonVoiceCommand(voiceCommand: String?, context: Context) {
        saveVariableInMemory(context, "distress_button_voice_command", voiceCommand)
    }

    fun getGoldNumberVoiceCommand(context: Context?): String? {
        return loadVariableFromMemory("gold_number_voice_command", context)
    }

    fun saveGoldNumberVoiceCommand(voiceCommand: String?, context: Context) {
        saveVariableInMemory(context, "gold_number_voice_command", voiceCommand)
    }

    fun getUnlockDeviceVoiceCommand(context: Context?): String? {
        return loadVariableFromMemory("unlock_device_voice_command", context)
    }

    fun saveUnlockDeviceVoiceCommand(voiceCommand: String?, context: Context) {
        saveVariableInMemory(context, "unlock_device_voice_command", voiceCommand)
    }

    fun loadAlreadyPlayedWelcomeSpeech(context: Context): Boolean {
        val alreadyPlayedWelcomeSpeech = loadVariableFromMemory("already_played_welcome_speech", context)
        return alreadyPlayedWelcomeSpeech == "true"
    }

    fun saveAlreadyPlayedWelcomeSpeech(context: Context, alreadyPlayedWelcomeSpeech: Boolean) {
        val varToSave = if (alreadyPlayedWelcomeSpeech) "true" else "false"
        saveVariableInMemory(context, "already_played_welcome_speech", varToSave)
    }

    fun loadDistressNumberOfSecsToCancel(context: Context): Long {
        val fallbackValue = context.resources.getString(R.string.distressNumberOfSecsToCancel).toLong()
        val distressNumberOfSecsToCancel = loadVariableFromMemory("distress_number_of_secs_to_cancel", context)
        return distressNumberOfSecsToCancel?.toLongOrNull() ?: fallbackValue
    }

    fun saveDistressNumberOfSecsToCancel(distressNumberOfSecsToCancel: String, context: Context) {
        val fallbackValue = context.resources.getString(R.string.distressNumberOfSecsToCancel).toLong()
        SettingsStatus.distressNumberOfSecsToCancel = distressNumberOfSecsToCancel.toLongOrNull() ?: fallbackValue
        saveVariableInMemory(context, "distress_number_of_secs_to_cancel", distressNumberOfSecsToCancel)
    }

    fun loadDistressNumberShouldAlsoTalk(context: Context): Boolean {
        val distressNumberShouldAlsoTalk = loadVariableFromMemory("distress_number_should_also_talk", context)
        if (distressNumberShouldAlsoTalk == null) {
            return context.resources.getBoolean(R.bool.distressNumberShouldAlsoTalk)
        }
        return distressNumberShouldAlsoTalk == "true"
    }

    fun saveDistressNumberShouldAlsoTalk(context: Context, distressNumberShouldAlsoTalk: Boolean) {
        val varToSave = if (distressNumberShouldAlsoTalk) "true" else "false"
        saveVariableInMemory(context, "distress_number_should_also_talk", varToSave)
    }

    fun loadIsAnswerCallsVoiceCommandEnabled(context: Context): Boolean {
        val isAnswerCallsVoiceCommandEnabled = loadVariableFromMemory("is_answer_calls_voice_command_enabled", context)
        if (isAnswerCallsVoiceCommandEnabled == null) {
            return context.resources.getBoolean(R.bool.isAnswerCallsVoiceCommandEnabled)
        }
        return isAnswerCallsVoiceCommandEnabled == "true"
    }

    fun saveIsAnswerCallsVoiceCommandEnabled(context: Context, isAnswerCallsVoiceCommandEnabled: Boolean) {
        val varToSave = if (isAnswerCallsVoiceCommandEnabled) "true" else "false"
        saveVariableInMemory(context, "is_answer_calls_voice_command_enabled", varToSave)
    }

    fun loadIsGoldNumberVoiceCommandEnabled(context: Context): Boolean {
        val isGoldNumberVoiceCommandEnabled = loadVariableFromMemory("is_gold_number_voice_command_enabled", context)
        if (isGoldNumberVoiceCommandEnabled == null) {
            return context.resources.getBoolean(R.bool.isGoldNumberVoiceCommandEnabled)
        }
        return isGoldNumberVoiceCommandEnabled == "true"
    }

    fun saveIsGoldNumberVoiceCommandEnabled(context: Context, isGoldNumberVoiceCommandEnabled: Boolean) {
        val varToSave = if (isGoldNumberVoiceCommandEnabled) "true" else "false"
        saveVariableInMemory(context, "is_gold_number_voice_command_enabled", varToSave)
    }

    fun loadIsQuickCallVoiceCommandEnabled(context: Context): Boolean {
        val isQuickCallVoiceCommandEnabled = loadVariableFromMemory("is_quick_call_voice_command_enabled", context)
        if (isQuickCallVoiceCommandEnabled == null) {
            return context.resources.getBoolean(R.bool.isQuickCallVoiceCommandEnabled)
        }
        return isQuickCallVoiceCommandEnabled == "true"
    }

    fun saveShouldSpeakWhenRing(context: Context, shouldSpeakWhenRing: Boolean) {
        val varToSave = if (shouldSpeakWhenRing) "true" else "false"
        saveVariableInMemory(context, "should_speak_when_ring", varToSave)
    }

    fun loadShouldSpeakWhenRing(context: Context): Boolean {
        val shouldSpeakWhenRing = loadVariableFromMemory("should_speak_when_ring", context)
        if (shouldSpeakWhenRing == null) {
            return context.resources.getBoolean(R.bool.shouldSpeakWhenRing)
        }
        return shouldSpeakWhenRing == "true"
    }

    fun saveIsQuickCallVoiceCommandEnabled(context: Context, isQuickCallVoiceCommandEnabled: Boolean) {
        val varToSave = if (isQuickCallVoiceCommandEnabled) "true" else "false"
        saveVariableInMemory(context, "is_quick_call_voice_command_enabled", varToSave)
    }

    fun loadIsUnlockScreenVoiceCommandEnabled(context: Context): Boolean {
        val isUnlockScreenVoiceCommandEnabled = loadVariableFromMemory("is_unlock_screen_voice_command_enabled", context)
        if (isUnlockScreenVoiceCommandEnabled == null) {
            return context.resources.getBoolean(R.bool.isUnlockScreenVoiceCommandEnabled)
        }
        return isUnlockScreenVoiceCommandEnabled == "true"
    }

    fun saveIsUnlockScreenVoiceCommandEnabled(context: Context, isUnlockScreenVoiceCommandEnabled: Boolean) {
        val varToSave = if (isUnlockScreenVoiceCommandEnabled) "true" else "false"
        saveVariableInMemory(context, "is_unlock_screen_voice_command_enabled", varToSave)
    }

    fun loadDistressButtonShouldAlsoSendSmsToGoldNumber(context: Context): Boolean {
        val quickCallAlsoSendSmsToGoldNumber = loadVariableFromMemory("quick_call_should_also_send_sms_to_gold_number", context)
        if (quickCallAlsoSendSmsToGoldNumber == null) {
            return context.resources.getBoolean(R.bool.quickCallShouldAlsoSendSmsToGoldNumber)
        }
        return quickCallAlsoSendSmsToGoldNumber == "true"
    }

    fun saveQuickCallShouldAlsoSendSmsToGoldNumber(context: Context, quickCallAlsoSendSmsToGoldNumber: Boolean) {
        val varToSave = if (quickCallAlsoSendSmsToGoldNumber) "true" else "false"
        saveVariableInMemory(context, "quick_call_should_also_send_sms_to_gold_number", varToSave)
    }
}
