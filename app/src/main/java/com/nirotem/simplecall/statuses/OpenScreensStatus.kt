package com.nirotem.simplecall.statuses

import androidx.lifecycle.MutableLiveData

//import io.reactivex.subjects.BehaviorSubject
//import timber.log.Timber

object OpenScreensStatus {
    var isSettingsScreenOpened = false
    var isPermissionsScreenOpened = false
    var isHelpScreenOpened = false
    var isPremiumTourScreenOpened = false
    var isCallReportScreenOpened = false
    var registerSettingsInstanceValue = 0
    var registerPermissionsInstanceValue = 0
    var registerCallReportInstanceValue = 0
    var registerPremiumTourInstanceValue = 0
    var shouldUpdateSettingsScreens = MutableLiveData(false)
    var shouldCloseSettingsScreens = MutableLiveData(0)
    var shouldClosePermissionsScreens = MutableLiveData(0)
    var shouldCloseCallReportScreens = MutableLiveData(0)
    var shouldClosePremiumTourScreens = MutableLiveData(0)
    var alreadyShownContactsClickOnRowForMore = false
    var alreadyShownCallsClickOnRowForMore = false
    var eventScreenActivityIsOpen = MutableLiveData(false)

}