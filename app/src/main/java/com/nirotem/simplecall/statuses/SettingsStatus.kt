package com.nirotem.simplecall.statuses

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.nirotem.simplecall.R
import interfaces.DescriptiveEnum


//import io.reactivex.subjects.BehaviorSubject
//import timber.log.Timber


enum class AllowAnswerCallsEnum(@StringRes override val descriptionRes: Int): DescriptiveEnum {
    NO_ONE(R.string.from_no_one_capital_f),
    FAVOURITES_ONLY(R.string.from_favorites_only_captial_f),
    CONTACTS_ONLY(R.string.from_contacts_only_captial_f),
    IDENTIFIED_ONLY(R.string.from_identified_only_captial_f),
    FROM_EVERYONE(R.string.from_everyone_capital_f)
}

/*enum class AllowAnswerCallsEnum2(@StringRes val descriptionRes: Int) {
    NO_ONE(R.string.allow_no_one),
    FAVOURITES_ONLY(R.string.allow_favorites_only),
    CONTACTS_ONLY(R.string.allow_contacts_only),
    IDENTIFIED_ONLY(R.string.allow_identified_only),
    FROM_EVERYONE(R.string.allow_from_everyone)
}*/

enum class AllowOutgoingCallsEnum(@StringRes override val descriptionRes: Int): DescriptiveEnum {
    NO_ONE(R.string.to_no_one_captial_t),
    FAVOURITES_ONLY(R.string.to_favorites_only_captial_t),
    CONTACTS_ONLY(R.string.to_contacts_only_captial_t),
    TO_EVERYONE(R.string.to_everyone_capital_t)
}

/*enum class AllowOutgoingCallsEnum(val description: String) {
    NO_ONE("Don't Allow Outgoing Calls"),
    FAVOURITES_ONLY("To Favorites Only"),
    CONTACTS_ONLY("To Contacts Only"),
    TO_EVERYONE("To Everyone")
}*/

enum class LanguagesEnum(val codes: List<String>, val region: String) {
    ENGLISH(listOf("en", "eng"), "US"),
    HEBREW(listOf("he", "iw", "heb"), "IL"),
    RUSSIAN(listOf("ru", "rus"), "RU"),
    FRENCH(listOf("fr", "fra", "fre"), "FR"),
    DUTCH(listOf("nl", "dut", "nld"), "NL"),
    SPANISH(listOf("es", "esp", "spa"), "ES"),
    GERMAN(listOf("de", "deu", "ger"), "DE"),
    ARABIC(listOf("ar", "ara", "arab"), "SA"),
    CHINESE(listOf("zh", "zho", "chi"), "CN"),
    INDONESIAN(listOf("id", "ind"), "ID"),
    HINDI(listOf("hi", "hin"), "IN"),
    KOREAN(listOf("ko", "kor"), "KR"),
    JAPANESE(listOf("ja", "jpn"), "JP");

    companion object {
        /**
         * Returns the corresponding LanguagesEnum for the given language code.
         * Defaults to ENGLISH if no match is found.
         */
        fun fromCode(code: String?): LanguagesEnum {
            if (code == null) return ENGLISH
            return values().firstOrNull { language ->
                language.codes.any { it.equals(code, ignoreCase = true) }
            } ?: ENGLISH
        }
    }
}

object SettingsStatus {
    var userAllowOutgoingCallsEnum =
        MutableLiveData<AllowOutgoingCallsEnum>(AllowOutgoingCallsEnum.NO_ONE) // Needed to answer calls
    var quickCallNumber = MutableLiveData<String?>(null)
    var quickCallNumberContact = MutableLiveData<String?>(null)
    var goldNumber = MutableLiveData<String?>(null)
    var goldNumberContact = MutableLiveData<String?>(null)
    var allowOpeningWhatsApp = MutableLiveData(false)
    var currLanguage = MutableLiveData<LanguagesEnum>(LanguagesEnum.ENGLISH)
    var isRightToLeft = MutableLiveData(false)
    var distressNumberOfSecsToCancel = 5L
    var isPremium = false
    var alreadyShowedBlockedMsg = false
    var alreadyShownPermissionGoldNumberMsg = false
    var alreadyShownQuickCallButWithoutPermissionMsg = false
    var appLogoResourceSmall = R.drawable.goldappiconphoneblack
}


/*    var sendCallsReportNumber =
        MutableLiveData(false) // Needed for Contacts screen and Add New Contact and Gold Number and Call Details/History
    var sendCallsReportContact = MutableLiveData(false) // Needed to add new Contact
    var sendCallsReportHoursInterval =
        MutableLiveData(false) // For Calls screen, Single Call history, Contact Calls history
    var speakDialerOptions =
        MutableLiveData(false) // For Loading Activity when app is not loaded (Incoming call)
    var callsReportIsGoldNumber = MutableLiveData(false)
    var callsReportPhoneNumber = MutableLiveData<String?>(null)
    var callsReportContact = MutableLiveData<String?>(null)*/