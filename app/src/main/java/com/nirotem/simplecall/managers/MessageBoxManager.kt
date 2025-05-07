package com.nirotem.simplecall.managers

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nirotem.simplecall.R
import com.google.android.material.snackbar.Snackbar
import java.util.PriorityQueue


object MessageBoxManager {
    enum class MessageBoxPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    data class MessageBoxMessage(
        val priority: MessageBoxPriority,
        val totalDuration: Long,
        var remainingTime: Long,
        val title: String,
        val text: String,
        val messageBoxContainerView: FrameLayout,
        val messageBoxTitleView: TextView,
        val messageBoxTxtView: TextView
    )

    // תור הודעות לפי עדיפות (לדוגמה, עדיפות גבוהה = ערך גבוה יותר)
    val messageQueue = PriorityQueue<MessageBoxMessage> { m1, m2 ->
        // השוואה לפי ordinal – HIGH יהיה בעל ordinal גבוה יותר
        m2.priority.ordinal - m1.priority.ordinal
    }

    var currentMessage: MessageBoxMessage? = null
    var elapsedTime: Long = 0L

    fun displayNextMessage() {
        if (currentMessage == null && messageQueue.isNotEmpty()) {
            currentMessage = messageQueue.poll()
            elapsedTime = 0L
            showMessage(currentMessage!!)
            startTimerForCurrentMessage()
        }
    }

    fun startTimerForCurrentMessage() {
        val timerInterval = 100L // בדיקה כל 100 מיליסקונד
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                currentMessage?.let {
                    elapsedTime += timerInterval
                    it.remainingTime = it.totalDuration - elapsedTime
                    if (it.remainingTime <= 0) {
                        hideMessage(it)
                        currentMessage = null
                        displayNextMessage()
                    } else {
                        handler.postDelayed(this, timerInterval)
                    }
                }
            }
        }
        handler.postDelayed(runnable, timerInterval)
    }

    fun onNewMessageReceived(newMessage: MessageBoxMessage) {
        if (currentMessage == null) {
            currentMessage = newMessage
            elapsedTime = 0L
            showMessage(newMessage)
            startTimerForCurrentMessage()
        } else if (newMessage.priority > currentMessage!!.priority) {
            // עדכון זמן ההצגה שנותר בהודעה הנוכחית
            currentMessage!!.remainingTime -= elapsedTime
            if (currentMessage!!.remainingTime > 0) {
                messageQueue.add(currentMessage!!)
            }
            // הצבת ההודעה החדשה כ—currentMessage
            currentMessage = newMessage
            elapsedTime = 0L
            showMessage(newMessage)
            startTimerForCurrentMessage()
        } else {
            // הודעה חדשה עם עדיפות נמוכה יותר מתווספת לתור
            messageQueue.add(newMessage)
        }
    }

    // פונקציות להצגת והסתרת הודעות
    fun showMessage(message: MessageBoxMessage) {
        message.messageBoxTitleView.text = message.title
        message.messageBoxTxtView.text = message.text
        message.messageBoxContainerView.visibility = VISIBLE
    }

    fun hideMessage(message: MessageBoxMessage) {
        message.messageBoxContainerView.visibility = GONE
    }


    fun showLongSnackBar(
        context: Context,
        message: String,
        duration: Int? = null,
        anchorView: View? = null
    ): Snackbar? {
        var snackBar: Snackbar? = null
        val length = duration ?: 8000
        try {
            // אם anchorView לא סופק, ננסה לקבל את ה-view הראשי מה-Activity
            val baseView = anchorView ?: (context as? Activity)?.findViewById(android.R.id.content)
            ?: throw IllegalArgumentException("No suitable view for displaying the snackbar")


            snackBar = Snackbar.make(baseView, message, length)
            val snackBarView = snackBar.view
            val textView = snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.maxLines = 8 // מבטיח שהטקסט לא ייחתך

            val isRTL = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

// השגת האייקון מהמשאבים והגדרת גודל קבוע (24dp)
            val iconDrawable = ContextCompat.getDrawable(context, R.drawable.goldappiconphoneblack)
            val sizeInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                context.resources.displayMetrics
            ).toInt()
            iconDrawable?.setBounds(0, 0, sizeInPx, sizeInPx)

// הגדרת האייקון לפי RTL או LTR
            if (isRTL) {
                textView.setCompoundDrawables(null, null, iconDrawable, null) // אייקון מימין בטקסט עברי
            } else {
                textView.setCompoundDrawables(iconDrawable, null, null, null) // אייקון משמאל בטקסט לועזי
            }

            textView.compoundDrawablePadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics
            ).toInt()

            snackBar.show()
        }
        catch (e: Exception) {
            Log.e("SimplyCall - MessageBoxManager", "showLongSnackBar Error (${e.message})")

            try {
                if (anchorView != null) {
                    snackBar = Snackbar.make(anchorView, message, length)
                }
                else {
                    val viewTemp: View? = (context as? Activity)?.findViewById(android.R.id.content)
                    if (viewTemp != null) {
                        snackBar = Snackbar.make(viewTemp, message, length)
                    }
                }
            }
            catch (e: Exception) {
                Log.e("SimplyCall - MessageBoxManager", "showLongSnackBar Snackbar.make 2nd Error (${e.message})")
            }
        }

        return snackBar
    }


   /* fun showCustomToast(context: Context, message: String, durationInMilliseconds: Int) {
        // Inflate the custom toast layout (assumes you have a layout file named custom_toast.xml with a TextView whose id is "text")
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)
        val textView = layout.findViewById<TextView>(R.id.text)
        textView.text = message

        // Create the Toast with the custom layout
        val toast = Toast(context)
        toast.view = layout
        // We use LENGTH_SHORT (≈2000 ms) as the base duration for repeated display
        toast.duration = Toast.LENGTH_SHORT

        val toastDisplayTime = 10000L // approximate duration of Toast.LENGTH_SHORT in milliseconds
        val startTime = System.currentTimeMillis()
        val handler = Handler(Looper.getMainLooper())

        // Runnable to repeatedly show the toast until the desired duration has passed
        val runnable = object : Runnable {
            override fun run() {
                toast.show()
                if (System.currentTimeMillis() - startTime < durationInMilliseconds) {
                    handler.postDelayed(this, toastDisplayTime)
                }
            }
        }
        handler.post(runnable)
    }*/

    fun showCustomToastDialog(context: Context, message: String, durationInMilliseconds: Long = 8000) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        val textView = layout.findViewById<TextView>(R.id.toast_text)
        val iconView = layout.findViewById<ImageView>(R.id.toast_icon)

        textView.text = message
        iconView.setImageResource(R.drawable.goldappiconphoneblack)

        Toast(context).apply {
            setGravity(Gravity.CENTER, 0, 0)
            setDuration(Toast.LENGTH_LONG)
            view = layout
            show()
        }

        // inflate the custom dialog layout
/*        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.custom_toast, null)

        // עדכון הטקסט בהודעה
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
        messageTextView.text = message

        // יצירת AlertDialog מותאם
        val builder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false) // אין אפשרות לסגור אותו ידנית, אלא רק אוטומטית

        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()

        // סגירת הדיאלוג אוטומטית לאחר הזמן המבוקש
        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, durationInMilliseconds)*/
    }

   /* fun showCustomToastPopup(context: Context, message: String, durationInMilliseconds: Long = 4000) {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.custom_toast, null)

        val messageTextView = popupView.findViewById<TextView>(R.id.dialog_message)
        messageTextView.text = message

        // יצירת PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // נעלם בלחיצה בכל מקום מחוץ לו
        )

        // מאפשר שקיפות ברקע של ה-PopupWindow
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // הצגה של ה-popup במרכז המסך
        popupView.post {
            popupWindow.showAtLocation(popupView.rootView, Gravity.CENTER, 0, 0)
        }

        popupView.visibility = VISIBLE

        // סגירה אוטומטית לאחר הזמן הרצוי
        Handler(Looper.getMainLooper()).postDelayed({
            if (popupWindow.isShowing) {
                popupWindow.dismiss()
            }
        }, durationInMilliseconds)
    }*/

}
