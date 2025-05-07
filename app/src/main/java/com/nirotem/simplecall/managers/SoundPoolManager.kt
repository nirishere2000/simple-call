package com.nirotem.simplecall.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.nirotem.simplecall.R

object SoundPoolManager {
    var incomingCallWaitingSoundName: String = "incomingWaitingCallSound"
    var incomingCallSoundName: String = "incomingCallSound"
    var welcomeInstructionsSoundName: String = "welcomeInstructionsSound"
    var callDisconnectedSoundName: String = "callDisconnectedSound"
    var waitingCallDisconnectedSoundName: String = "waitingCallDisconnectedSound"
    var byeByeGreetingSoundName: String = "byeByeGreetingSound"
    var digit0SoundName: String = "digit0Sound"
    var digit1SoundName: String = "digit1Sound"
    var digit2SoundName: String = "digit2Sound"
    var digit3SoundName: String = "digit3Sound"
    var digit4SoundName: String = "digit4Sound"
    var digit5SoundName: String = "digit5Sound"
    var digit6SoundName: String = "digit6Sound"
    var digit7SoundName: String = "digit7Sound"
    var digit8SoundName: String = "digit8Sound"
    var digit9SoundName: String = "digit9Sound"
    var asteriskSoundName: String = "asteriskSound"
    var poundKeySoundName: String = "poundKeySound"
    var keyClickSoundName: String = "keyClickSound"

   var incomingCallSoundFileFinishedLoading = false

    private lateinit var soundPool: SoundPool
    private val soundMap: MutableMap<String, Int> = mutableMapOf()
    private val streamIds: MutableMap<String, Int> = mutableMapOf()
    private var incomingCallSoundId: Int = 0
    private var incomingCallSoundWasTriedToLoadBeforeInitCompleted = false
    private var shouldPlayVoiceSounds = false

    /**
     * Initializes the SoundPoolManager with a SoundPool instance.
     * Call this once in the Application class or similar initialization code.
     */
    fun initialize(context: Context) {
        shouldPlayVoiceSounds = context.resources.getBoolean(R.bool.playVoiceSound)
        // incoming call might not be loaded before using - we we need to know
        if (!::soundPool.isInitialized) {
            incomingCallSoundFileFinishedLoading = false
            incomingCallSoundWasTriedToLoadBeforeInitCompleted = false
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(5) // Maximum number of simultaneous streams
                .build()
        }
        if (!incomingCallSoundFileFinishedLoading) {
            soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) { // 0 indicates success
                    //incomingCallSoundFileFinishedLoading
                    if (sampleId == incomingCallSoundId) {
                        incomingCallSoundFileFinishedLoading = true
                        if (incomingCallSoundWasTriedToLoadBeforeInitCompleted && shouldPlayVoiceSounds) {
                            playSound(incomingCallSoundName, true)
                        }
                    }
                    //Log.d("SoundPoolManager", "Sound $sampleId loaded successfully")
                } else {
                    Log.e("SoundPoolManager", "Failed to load sound $sampleId")
                }
            }

            incomingCallSoundId = loadSound(context, incomingCallSoundName, R.raw.alternative_ringing_sound)
            loadSound(context, welcomeInstructionsSoundName, R.raw.welcome_instructions)
            loadSound(context, incomingCallWaitingSoundName, R.raw.incoming_call_waiting)
            loadSound(context, callDisconnectedSoundName, R.raw.call_disconnected)
            loadSound(context, waitingCallDisconnectedSoundName, R.raw.call_waiting_disconnected)

            loadSound(context, digit0SoundName, R.raw.digit0)
            loadSound(context, digit1SoundName, R.raw.digit1)
            loadSound(context, digit2SoundName, R.raw.digit2)
            loadSound(context, digit3SoundName, R.raw.digit3)
            loadSound(context, digit4SoundName, R.raw.digit4)
            loadSound(context, digit5SoundName, R.raw.digit5)
            loadSound(context, digit6SoundName, R.raw.digit6)
            loadSound(context, digit7SoundName, R.raw.digit7)
            loadSound(context, digit8SoundName, R.raw.digit8)
            loadSound(context, digit9SoundName, R.raw.digit9)
            loadSound(context, poundKeySoundName, R.raw.sulamit)
            loadSound(context, asteriskSoundName, R.raw.kochavit)
            loadSound(context, keyClickSoundName, R.raw.click)
        }
    }

    /**
     * Loads a sound resource into the SoundPool and associates it with a name.
     * @param context The application context.
     * @param name The name of the sound to reference later.
     * @param resId The resource ID of the sound file (e.g., R.raw.sound1).
     */
    private fun loadSound(context: Context, name: String, resId: Int): Int {
        if (!::soundPool.isInitialized) throw IllegalStateException("SoundPoolManager not initialized")
        val soundId = soundPool.load(context, resId, 1)
        soundMap[name] = soundId
        Log.d("SimplyCall - SoundPoolManager", "Loaded $name sound file (sound id: $soundId)")
        return soundId
    }

    /**
     * Plays a sound by its name. If not found, the method will do nothing.
     * @param name The name of the sound to play.
     * @param loop Whether to loop the sound (-1 for infinite loop, 0 for no loop).
     */
    fun playSound(name: String, loop: Boolean = false) {
        if (!::soundPool.isInitialized) throw IllegalStateException("SoundPoolManager not initialized")
        val soundId = soundMap[name] ?: return
        val streamId = soundPool.play(soundId, 1f, 1f, 1, if (loop) -1 else 0, 1f)

        if (name === incomingCallSoundName && !incomingCallSoundFileFinishedLoading) {
            incomingCallSoundWasTriedToLoadBeforeInitCompleted = true
        }

        streamIds[name] = streamId
        Log.d("SimplyCall - SoundPoolManager", "Played $name sound file and got ID $streamId")
    }

    /**
     * Stops a sound that is currently playing by its name.
     * @param name The name of the sound to stop.
     */
    fun stopSound(name: String) {
        if (!::soundPool.isInitialized) throw IllegalStateException("SoundPoolManager not initialized")
        val streamId = streamIds[name] ?: return
        soundPool.stop(streamId)
        streamIds.remove(name)
    }

    /**
     * Releases the SoundPool resources. Should be called when the app is closing or the sounds are no longer needed.
     */
    fun release() {
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
    }
}
