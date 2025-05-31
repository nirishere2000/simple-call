package com.nirotem.voicerecognition

import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    val lastCommand = MutableLiveData<String>("")
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var listenIntent: Intent
    private val handler = Handler(Looper.getMainLooper())
    private const val restartDelay = 250L    // פער קצר בין סשנים
    //private var currContext: Context? = null

    fun initVoiceCommands(context: Context, activity: AppCompatActivity? = null): Boolean {
        //if (!speechCommandsEnabled) { return false }
        //currContext = context
        // בקשת הרשאה להקלטה אם אין

        if (ActivityCompat.checkSelfPermission(context, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (activity != null) {
               // ActivityCompat.requestPermissions(activity, arrayOf(RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            } else {
               Toast.makeText(context, "No permission", Toast.LENGTH_LONG).show()
            }
            return false
        }

        return true
    }




  /*  fun startListenToVoiceCommands(context: Context) {
        if (!speechCommandsEnabled) { return }
        lastCommand.value = ""

        // יצירת מופע של SpeechRecognizer והגדרת RecognitionListener
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(this)

        // הכנת Intent להקשבה
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Please say voice command")
           // putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 50000000);
             putExtra("android.speech.extra.DICTATION_MODE", true);
             putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 20000) // 20 שניות
       //putExtra(RecognizerIntent.EXTRA_SPEECH_INPU  T_MINIMUM_LENGTH_MILLIS, 20000) // 20 שניות
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
                    *//*            if (partialCommand.contains("המילה_הרצויה", ignoreCase = true)) {
                                    // המילה זוהתה – נעצור הקשבה ונעביר לטיפול
                                    speechRecognizer.stopListening()
                                    handleVoiceCommand(partialCommand)
                                }*//*
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
    }*/

    /*   הפונקציה שאתה קורא מבחוץ  */
    fun startListenToVoiceCommands(context: Context) {
      //  if (!speechCommandsEnabled) return
        lastCommand.postValue("")

        /* 1. יצירת מופע SpeechRecognizer – אם אפשר, מנוע On-device  */
        speechRecognizer =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }

        /* 2. Intent אחד שנשמור לכל הסשנים  */
        val listenIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Please say voice command")

            // טיפים שהוספנו:
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)          // לקבל טקסט תוך-כדי
            putExtra("android.speech.extra.DICTATION_MODE", true)          // מאריך סשן בחלק מהמכשירים
            putExtra("android.speech.extra.PREFER_OFFLINE", true)          // אופליין אם זמין
            putExtra("android.speech.extra.SUPPRESS_BEEP", true)           // מבטל ביפ (יצרנים מסוימים)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 20_000)
        }

        /* 3. מאזין קבוע עם RESTART פנימי */
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            // 3.a אירועים בסיסיים
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            // 3.b סוף דיבור – מפעילים מחדש
            override fun onEndOfSpeech() = restartListening()

            // 3.c תוצאה מלאה
            override fun onResults(results: Bundle) {
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { handleVoiceCommand(it) }

                restartListening()
            }

            // 3.d תוצאות חלקיות
            override fun onPartialResults(partialResults: Bundle) {
                partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { handleVoiceCommand(it.trim()) }
            }

            // 3.e שגיאות – רובן נפתרות ע״י RESTART
            override fun onError(errorCode: Int) {
                when (errorCode) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        restartListening()

                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        handler.postDelayed({ restartListening() }, 500)

                    else ->
                        Log.e("SimplyCall-SR", "Fatal error $errorCode")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(listenIntent)
        Log.d("SimplyCall-SR", "Started Listening")
    }

    /*  🅒 פונקציית עזר להפעלה-מחדש */
    private fun restartListening() {
        handler.postDelayed({
            try {
                speechRecognizer.cancel()          // מבטל סשן קודם לגמרי
                speechRecognizer.startListening(listenIntent)
            } catch (e: Exception) {
                Log.e("SimplyCall-SR", "restart failed: ${e.message}")
            }
        }, restartDelay)
    }

    /*  🅓 ניקוי משאבים – לקרוא ב-onDestroy()  */
    fun stopVoiceCommands() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            speechRecognizer.cancel()
            speechRecognizer.destroy()
        }
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
