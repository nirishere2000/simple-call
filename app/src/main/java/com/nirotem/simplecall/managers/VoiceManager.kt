package com.nirotem.simplecall.managers

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadIsAnswerCallsVoiceCommandEnabled
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadIsQuickCallVoiceCommandEnabled
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadIsGoldNumberVoiceCommandEnabled
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadIsUnlockScreenVoiceCommandEnabled
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsAnswerCallsVoiceCommandEnabled
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsQuickCallVoiceCommandEnabled
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsGoldNumberVoiceCommandEnabled
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsUnlockScreenVoiceCommandEnabled
import com.nirotem.simplecall.statuses.PermissionsStatus.askForRecordPermission
import com.nirotem.simplecall.R

object VoiceManager {
    var speechCommandsEnabled = false // this is the real place where it's init and not initValues

    fun initVoiceCommandsSettings(view: View, activity: Activity) {
        // Answer calls command:
        val commandAnswerCallTextView = view.findViewById<TextView>(R.id.commandAnswerCalls)
        var voiceCommandAnswerCall =
            com.nirotem.simplecall.helpers.SharedPreferencesCache.getAnswerCallVoiceCommand(view.context)
        if (voiceCommandAnswerCall == null) {
            voiceCommandAnswerCall =
                view.context.getString(com.nirotem.simplecall.R.string.initial_voice_command_answer_calls)
        }
        commandAnswerCallTextView.text = view.context.getString(
            com.nirotem.simplecall.R.string.say_voice_command,
            voiceCommandAnswerCall
        )

        val commandToggleAnswerCalls =
            view.findViewById<SwitchMaterial>(R.id.command_toggle_answer_calls)
        val isCommandEnabled = loadIsAnswerCallsVoiceCommandEnabled(view.context)
        commandToggleAnswerCalls.isChecked = isCommandEnabled
        commandToggleAnswerCalls.setOnCheckedChangeListener { buttonView, isChecked ->
            saveIsAnswerCallsVoiceCommandEnabled(view.context, isChecked)
            askForRecordPermission(view.context, activity)
        }

        // Gold Number command:
        val commandGoldNumberTextView = view.findViewById<TextView>(R.id.commandGoldNumber)
        var voiceCommandGoldNumber =
            com.nirotem.simplecall.helpers.SharedPreferencesCache.getGoldNumberVoiceCommand(view.context)
        if (voiceCommandGoldNumber == null) {
            voiceCommandGoldNumber =
                view.context.getString(com.nirotem.simplecall.R.string.initial_voice_command_call_gold_number)
        }
        commandGoldNumberTextView.text = view.context.getString(
            com.nirotem.simplecall.R.string.say_voice_command,
            voiceCommandGoldNumber
        )

        val commandToggleGoldNumber =
            view.findViewById<SwitchMaterial>(R.id.command_toggle_gold_number)
        val isGoldNumberCommandEnabled = loadIsGoldNumberVoiceCommandEnabled(view.context)
        commandToggleGoldNumber.isChecked = isGoldNumberCommandEnabled
        commandToggleGoldNumber.setOnCheckedChangeListener { buttonView, isChecked ->
            saveIsGoldNumberVoiceCommandEnabled(view.context, isChecked)
            askForRecordPermission(view.context, activity)
        }

        // Distress button command:
        val commandDistressButtonTextView = view.findViewById<TextView>(R.id.commandDistressButton)
        var voiceCommandDistressButton =
            com.nirotem.simplecall.helpers.SharedPreferencesCache.getDistressButtonVoiceCommand(view.context)
        if (voiceCommandDistressButton == null) {
            voiceCommandDistressButton =
                view.context.getString(com.nirotem.simplecall.R.string.initial_voice_command_quick_call)
        }
        commandDistressButtonTextView.text = view.context.getString(
            com.nirotem.simplecall.R.string.say_voice_command,
            voiceCommandDistressButton
        )

        val commandToggleDistressButton =
            view.findViewById<SwitchMaterial>(R.id.command_toggle_distress_button)
        val isDistressButtonCommandEnabled = loadIsQuickCallVoiceCommandEnabled(view.context)
        commandToggleDistressButton.isChecked = isDistressButtonCommandEnabled
        commandToggleDistressButton.setOnCheckedChangeListener { buttonView, isChecked ->
            saveIsQuickCallVoiceCommandEnabled(view.context, isChecked)
            askForRecordPermission(view.context, activity)
        }

        // Unlock Screen command:
        val commandUnlockDeviceTextView = view.findViewById<TextView>(R.id.commandUnlockDevice)
        var voiceCommandUnlockDevice =
            com.nirotem.simplecall.helpers.SharedPreferencesCache.getUnlockDeviceVoiceCommand(view.context)
        if (voiceCommandUnlockDevice == null) {
            voiceCommandUnlockDevice =
                view.context.getString(com.nirotem.simplecall.R.string.initial_voice_command_unlock_screen)
        }
        commandUnlockDeviceTextView.text = view.context.getString(
            com.nirotem.simplecall.R.string.say_voice_command,
            voiceCommandUnlockDevice
        )

        val commandToggleUnlockScreen =
            view.findViewById<SwitchMaterial>(R.id.command_toggle_unlock_screen)
        val isUnlockScreenCommandEnabled = loadIsUnlockScreenVoiceCommandEnabled(view.context)
        commandToggleUnlockScreen.isChecked = isUnlockScreenCommandEnabled
        commandToggleUnlockScreen.setOnCheckedChangeListener { buttonView, isChecked ->
            saveIsUnlockScreenVoiceCommandEnabled(view.context, isChecked)
            askForRecordPermission(view.context, activity)
        }

        /*        val atLeastOnceCommandEnabled = commandToggleAnswerCalls.isChecked || commandToggleGoldNumber.isChecked
                        || commandToggleDistressButton.isChecked || commandToggleUnlockScreen.isChecked

                if (atLeastOnceCommandEnabled) {
                    askForRecordPermission(view.context, activity)
                }*/
    }
}

interface VoiceApi {
    fun isEnabled(): Boolean
    fun startListenToVoiceCommands(context: Context)
    fun initVoiceCommands(context: Context, activity: AppCompatActivity? = null): Boolean
    fun getLastCommand(): LiveData<String>
    fun updateLastCommand(cmd: String)
    fun stopListen()
}