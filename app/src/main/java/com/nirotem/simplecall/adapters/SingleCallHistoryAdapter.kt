package com.nirotem.simplecall.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri

import androidx.recyclerview.widget.RecyclerView
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.R
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.ui.singleCallHistory.SinglePhoneCall
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class SingleCallHistoryAdapter(context: Context?) :
    RecyclerView.Adapter<SingleCallHistoryAdapter.CallViewHolder>() {
   // private val loadedRecords = callHistory
    private val callHistoryList = mutableListOf<SinglePhoneCall>()
    private val fragmentContext = context

/*    init {
        if (loadedRecords.isNotEmpty()) {
            addItems(loadedRecords)
        }
    }*/

    class CallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {



        //    val callTypeIcon: ImageView = itemView.findViewById(R.id.callTypeIcon)
        val callDate: TextView = itemView.findViewById(R.id.callDate)
        val missedIndicator: ImageView =
            itemView.findViewById(R.id.missedIndicator) // A red dot or similar
        val detailsCaption: TextView = itemView.findViewById(R.id.detailsCaption)
    }

    fun historyListSize(): Int {
        return callHistoryList.size
    }

    fun addItems(newItems: List<SinglePhoneCall>) {
        val startPosition = callHistoryList.size
        callHistoryList.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
       // callHistoryList.addAll(callHistoryList)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_item_call, parent, false)
        return CallViewHolder(view)
    }

    private fun makeCall(callPhoneNumber: String) {
        //   if (checkSelfPermission(this, CALL_PHONE) == PERMISSION_GRANTED) {
        //val uri = "tel:${+97237537900}".toUri()

        val uri = "tel:${callPhoneNumber}".toUri()

        if (fragmentContext != null) {
            OutgoingCall.isCalling = true

            startActivity(fragmentContext, Intent(Intent.ACTION_CALL, uri), null)
        }
        //     } else {
        //        requestPermissions(this, arrayOf(CALL_PHONE), REQUEST_PERMISSION)
        //      }


    }

    fun waveAnimation(textView: TextView) {
// In the layout, use a shader for the TextView
// In the layout, use a shader for the TextView
        // val textView = this.findViewById<TextView>(R.id.textView)

        val textShader = LinearGradient(
            0f, 0f, textView.paint.measureText(textView.text.toString()), textView.textSize,
            intArrayOf(Color.RED, Color.BLUE, Color.GREEN),
            null,
            Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader

        // val textView = findViewById<TextView>(R.id.textView)
        /*    val colorAnimator = ObjectAnimator.ofArgb(textView, "textColor", Color.RED, Color.BLUE)
            colorAnimator.duration = 1000
            colorAnimator.repeatMode = ValueAnimator.REVERSE
            colorAnimator.repeatCount = ValueAnimator.INFINITE
            colorAnimator.start()*/
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        val call = callHistoryList[position]

        //    holder.callType.text = "Incoming" // call.type

        var dateFormat = "yyyy-dd-MM HH:mm"
        if (SettingsStatus.currLanguage.value == LanguagesEnum.ENGLISH) {
            dateFormat = "yyyy-MM-dd HH:mm"
        }

        if (SettingsStatus.currLanguage.value == LanguagesEnum.ARABIC) {
            holder.callDate.text = call.callDate
        }
        else {
            // קודם כל צריך לפרסר את המחרוזת לפי הפורמט שלה (נגיד "yyyy-MM-dd HH:mm")
            val originalFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val dateTime = LocalDateTime.parse(call.callDate, originalFormat)

            // עכשיו נגדיר Formatter "מקומי" לפי SHORT או MEDIUM (תלוי כמה פירוט רוצים)
            // - SHORT:    בפועל יציג מאוד מקוצר, לדוגמה "29.1.25, 20:27"
            // - MEDIUM:   קצת יותר מלא, למשל "29 בינו׳ 2025, 20:27"
            // - LONG/FULL יכולים לכלול פרטים נוספים כמו שם יום בשבוע או אזור זמן.
            val localizedFormatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                // כדי להשתמש בשפת המכשיר, עושים withLocale(Locale.getDefault()),
                // אם רוצים לכפות עברית: withLocale(Locale("he", "IL"))
                .withLocale(Locale.getDefault())

            // המרת התאריך שפורסר לתצוגה מקומית
            val localizedDateString = dateTime.format(localizedFormatter)


            holder.callDate.text = localizedDateString
        }

        // Set call type icon
        when (call.type) {
            "Incoming" -> holder.missedIndicator.setImageResource(R.drawable.incoming_call)
            "Outgoing" -> holder.missedIndicator.setImageResource(R.drawable.outgoing_call)
            "Missed" -> holder.missedIndicator.setImageResource(android.R.drawable.sym_call_missed)
            "Rejected" -> holder.missedIndicator.setImageResource(android.R.drawable.ic_delete)
            "Blocked" -> holder.missedIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
        }

        when (call.type) {
            "Incoming" -> holder.detailsCaption.text = fragmentContext?.getString(R.string.incoming_capital)
            "Outgoing" -> holder.detailsCaption.text = fragmentContext?.getString(R.string.outgoing_capital)
            "Missed" -> holder.detailsCaption.text = fragmentContext?.getString(R.string.missed_capital)
            "Rejected" -> holder.detailsCaption.text = fragmentContext?.getString(R.string.rejected_capital)
            "Blocked" -> holder.detailsCaption.text = fragmentContext?.getString(R.string.blocked_capital)
        }
        // holder.callTypeIcon.setImageResource(R.drawable.speaker_on)

        // Show or hide missed call indicator
        // holder.missedIndicator.visibility = if (call.missed) View.VISIBLE else View.GONE
        // holder.missedIndicator.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int = callHistoryList.size
}
