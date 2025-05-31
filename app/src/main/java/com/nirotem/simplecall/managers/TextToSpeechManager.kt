package com.nirotem.simplecall.managers

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
//import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar

import java.util.*

object TextToSpeechManager : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    var isTtsReady = false
        private set
    private var welcomeTitle = ""
    private var welcomeText = ""
    private val speechQueue = ArrayDeque<String>() // תור לשמירה על טקסטים לדיבור
    private var isSpeaking = false // דגל שמעיד אם הדיבור פעיל
    private var initOnSpeechCompleteCallBack: () -> Unit = {}


    fun initSpeech(context: Context, welcomeTitlePrm: String, welcomeTextPrm: String, onFinished: () -> Unit = {}) {
        welcomeTitle = welcomeTitlePrm // context.getString(R.string.welcome_to_easy_call_and_answer_premium)
        welcomeText = welcomeTextPrm // context.getString(R.string.welcome_text_speech)
        initOnSpeechCompleteCallBack = onFinished
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val currentLocale = Locale.getDefault()
            setLanguage(currentLocale)
            isTtsReady = true

            when {
                // יש כותרת וגם טקסט - שרשרת מלאה
                welcomeTitle.isNotEmpty() && welcomeText.isNotEmpty() -> {
                    addToQueueAndSpeak(welcomeTitle) {
                        addToQueueAndSpeak(welcomeText, initOnSpeechCompleteCallBack)
                    }
                }

                // רק כותרת
                welcomeTitle.isNotEmpty() -> {
                    addToQueueAndSpeak(welcomeTitle, initOnSpeechCompleteCallBack)
                }

                // אין כותרת - רק טקסט
                welcomeText.isNotEmpty() -> {
                    addToQueueAndSpeak(welcomeText, initOnSpeechCompleteCallBack)
                }

                // כלום? נפעיל מיד את הקול-בק החיצוני
                else -> initOnSpeechCompleteCallBack()
            }
        }
    }

    /** @param text        הטקסט להקראה
    * @param onFinished  פעולה שתופעל כש-TTS סיים (ברירת-המחדל: כלום)
    * @return            true אם ההקראה התחילה בהצלחה
    */
    fun speak(text: String, onFinished: () -> Unit = {}): Boolean {
        try {
            if (isTtsReady && !isSpeaking) { // רק אם לא מדברים כרגע
                val utteranceId = UUID.randomUUID().toString()
                isSpeaking = true // מציינים שאנחנו מתחילים לדבר
                textToSpeech?.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    utteranceId
                )

                // Set the listener for utterance completion
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // קרה כאשר הקריאה מתחילה
                    }

                    override fun onDone(utteranceId: String?) {
                        // קרה כאשר הקריאה הסתיימה
                        onSpeechFinished()  // הפעל את הפונקציה ברגע שהדיבור מסתיים
                        onFinished()
                    }

                    override fun onError(utteranceId: String?) {
                        // טיפול בשגיאה אם יש
                    }
                })
                return true
            }
        } catch (e: Exception) {
            // טפל בשגיאות אם יש
        }
        return false
    }

    fun speakOrSnackBar(text: String, context: Context) {
        if (!speak(text)) {
           // com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar(context, text)
        }
    }

    fun addToQueueAndSpeak(text: String, onFinished: () -> Unit = {}) {
        // הוספת הטקסט לתור
        speechQueue.add(text)
        if (!isSpeaking) {
            // אם אין דיבור פעיל, נתחיל לקרוא את הטקסט הראשון בתור
            speakNextInQueue(onFinished)
        }
    }

    private fun speakNextInQueue(onFinished: () -> Unit = {}) {
        // אם יש טקסט בתור, נקרוא אותו
        if (speechQueue.isNotEmpty()) {
            val nextText = speechQueue.poll() // קח את הטקסט הראשון בתור
            if (nextText != null) {
                speak(nextText, onFinished)
            }
        }
    }

    fun onSpeechFinished() {
        // אם סיימנו לקרוא טקסט, נתחיל לקרוא את הטקסט הבא בתור
        isSpeaking = false // שחרור הדגל
        speakNextInQueue()
    }

    fun setLanguage(locale: Locale): Boolean {
        val result = textToSpeech?.setLanguage(locale)
        return result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsReady = false
    }
}
