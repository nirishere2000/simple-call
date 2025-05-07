package com.nirotem.simplecall.managers

import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import java.util.Locale

object SpeakCommandsManager : RecognitionListener {

    var speechCommandsEnabled = false // this is the real place where it's init and not initValues
    var lastCommand = MutableLiveData("")
    private lateinit var speechRecognizer: SpeechRecognizer
    private val REQUEST_RECORD_AUDIO_PERMISSION = 1
    //private var currContext: Context? = null

    fun init(context: Context, activity: AppCompatActivity? = null): Boolean {
        if (!speechCommandsEnabled) { return false }
        //currContext = context
        // בקשת הרשאה להקלטה אם אין
        if (ActivityCompat.checkSelfPermission(context, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (activity != null) {
                ActivityCompat.requestPermissions(activity, arrayOf(RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            } else {
               Toast.makeText(context, "No permission", Toast.LENGTH_LONG).show()
            }
            return false
        }

        return true
    }

    fun startListen(context: Context) {
        if (!speechCommandsEnabled) { return }
        lastCommand.value = ""

        // יצירת מופע של SpeechRecognizer והגדרת RecognitionListener
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(this)

        // הכנת Intent להקשבה
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "נא להקליט פקודה קולית")
           // putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 50000000);
             putExtra("android.speech.extra.DICTATION_MODE", true);
             putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 20000) // 20 שניות
       //putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 20000) // 20 שניות
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(bytes: ByteArray?) {}
            override fun onEndOfSpeech() {
                // changing the color of our mic icon to
                // gray to indicate it is not listening

                Log.d(
                    "SimplyCall - SpeakCommandsManager",
                    "onEndOfSpeech"
                )

               // micIV.setColorFilter(ContextCompat.getColor(applicationContext, R.color.mic_disabled_color)) // #FF6D6A6A
            }

            override fun onError(errorCode: Int) {
                val message = when (errorCode) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    // Add other cases based on SpeechRecognizer error codes
                    else -> "Unknown error"
                }
                Log.e(
                    "SimplyCall - SpeakCommandsManager",
                    "onError (Error occurred: ${message})"
                )
                //Toast.makeText(applicationContext, "Error occurred: $message", Toast.LENGTH_SHORT).show()
            }


            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { voiceCommand ->
                    handleVoiceCommand(voiceCommand)
                }

                Log.d(
                    "SimplyCall - SpeakCommandsManager",
                    "onResults2 - $matches"
                )
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(
                    "SimplyCall - SpeakCommandsManager",
                    "onPartialResults2 - matches $matches"
                )

                matches?.firstOrNull()?.let { partialCommand ->

                    handleVoiceCommand(partialCommand.trim())
                    /*            if (partialCommand.contains("המילה_הרצויה", ignoreCase = true)) {
                                    // המילה זוהתה – נעצור הקשבה ונעביר לטיפול
                                    speechRecognizer.stopListening()
                                    handleVoiceCommand(partialCommand)
                                }*/
                }
            }

            override fun onEvent(i: Int, bundle: Bundle?) {}

        })

        speechRecognizer.startListening(intent)
     //Toast.makeText(context, "Started Listening", Toast.LENGTH_LONG).show()
        Log.d(
            "SimplyCall - SpeakCommandsManager",
            "Started Listening"
        )
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.firstOrNull()?.let { voiceCommand ->
            handleVoiceCommand(voiceCommand)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        Log.d(
            "SimplyCall - SpeakCommandsManager",
            "onPartialResults - matches $matches"
        )

        matches?.firstOrNull()?.let { partialCommand ->

            handleVoiceCommand(partialCommand)
/*            if (partialCommand.contains("המילה_הרצויה", ignoreCase = true)) {
                // המילה זוהתה – נעצור הקשבה ונעביר לטיפול
                speechRecognizer.stopListening()
                handleVoiceCommand(partialCommand)
            }*/
        }
    }


    override fun onError(error: Int) {
        //Toast.makeText(currContext, "Error: $error", Toast.LENGTH_LONG).show()
        Log.e(
            "SimplyCall - SpeakCommandsManager",
            "onError (Error Code: ${error})"
        )
    }

    private fun handleVoiceCommand(command: String) {
        Log.d(
            "SimplyCall - SpeakCommandsManager",
            "handleVoiceCommand (command: ${command})"
        )
        lastCommand.value = command

/*        currContext?.let {
            Toast.makeText(it, "פקודה: $command", Toast.LENGTH_LONG).show()
        }*/
    }

    fun stopListen() {
       // lastCommand.value = "" could reset before time
        try {
            if (::speechRecognizer.isInitialized) { // don't count on enable/disable because if it can change to false in middle of app run
                // then we'll still need to stop
                speechRecognizer.stopListening()
                Log.d(
                    "SimplyCall - SpeakCommandsManager",
                    "stopListen"
                )
            }
        }
        catch (e: Exception) {
            Log.e(
                "SimplyCall - SpeakCommandsManager",
                "stopListen-Error (${e.message})"
            )
        }
    }

    // שאר המתודות של RecognitionListener (ניתן להשאיר ריקות או להוסיף לוגיקה לפי הצורך)
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
   // override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
