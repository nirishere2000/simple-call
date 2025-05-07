/*
package com.nirotem.simplecall.adapters

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController

import androidx.recyclerview.widget.RecyclerView
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R

import com.nirotem.simplecall.ui.contacts.ContactsInLetterListItem
import com.nirotem.simplecall.ui.singleCallHistory.SingleCallHistoryViewModel

class ContactsAdapter(
    private val contactsListDTO: List<ContactsInLetterListItem>,
    context: Context?,
    navigationController: NavController,
    sharedSingleCallViewModel: SingleCallHistoryViewModel
) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private val fragmentContext = context
    private val navController = navigationController
    private val singleCallViewModel = sharedSingleCallViewModel

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactName: TextView = itemView.findViewById(R.id.contactsContactName)

        //    val callTypeIcon: ImageView = itemView.findViewById(R.id.callTypeIcon)
        val lastContactedDate: TextView = itemView.findViewById(R.id.lastContactedDate)
        val contactIndicator: ImageView =
            itemView.findViewById(R.id.contactIndicator) // for now is favorite
        val callButton: ImageView = itemView.findViewById(R.id.contactsCallButton)
        val infoButton: ImageView = itemView.findViewById(R.id.contactsInfoButton)
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)

        fun bind(header: ContactsInLetterListItem.Header) {
            headerText.text = header.letter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_row, parent, false)
        return ContactViewHolder(view)
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
        */
/*    val colorAnimator = ObjectAnimator.ofArgb(textView, "textColor", Color.RED, Color.BLUE)
            colorAnimator.duration = 1000
            colorAnimator.repeatMode = ValueAnimator.REVERSE
            colorAnimator.repeatCount = ValueAnimator.INFINITE
            colorAnimator.start()*//*



    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ContactsInLetterListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ContactsInLetterListItem.Contact -> (holder as ContactViewHolder).bind(item)
        }
    }


    fun onBindViewHolder2(holder: ContactViewHolder, position: Int) {
        val contact = contactsListDTO[position]
        holder.contactName.text = contact.contactOrPhoneNumber
        holder.lastContactedDate.text = contact.phoneNumber
        val photoUri = contact.photoUri
        val isFavourite = contact.isFavourite

        if (photoUri !== "") {
            //  val photoUri2 = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
            val uri = Uri.parse(photoUri)

            if (uri.scheme == "content") {
                // It's a content URI (e.g., contact photo URI)
                holder.contactIndicator.setImageURI(uri)
            } else if (uri.scheme == "file") {
                // It's a file URI (direct path to the image)
                val bitmap = BitmapFactory.decodeFile(uri.path)
                holder.contactIndicator.setImageBitmap(bitmap)
            } else {
                // Handle other URI schemes, if necessary
                if (isFavourite) {
                    holder.contactIndicator.setImageResource(android.R.drawable.star_big_on)
                } else {
                    holder.contactIndicator.visibility = GONE
                }
            }

            // holder.contactIndicator.setImageResource(photoUri)

        } else {
            // No photo URI available, set a default image
            if (isFavourite) {
                holder.contactIndicator.setImageResource(android.R.drawable.star_big_on)
            } else {
                holder.contactIndicator.visibility = GONE
            }
        }
        val noPermissionToCall =
            PermissionsStatus.callPhonePermissionGranted.value === null || (!(PermissionsStatus.callPhonePermissionGranted.value!!))

        holder.callButton.visibility =
            if (contact.phoneNumber === "" || noPermissionToCall) INVISIBLE else VISIBLE
        if (contact.phoneNumber !== "" && (!noPermissionToCall)) {
            holder.callButton.setOnClickListener({
                makeCall(contact.phoneNumber)
            })
        }

        holder.infoButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("phone_number", contact.phoneNumber)
                putString("contact", contact.contactOrPhoneNumber)
            }
            singleCallViewModel.setSharedData(
                SingleCallHistoryViewModel.ContactData(
                    contact.phoneNumber,
                    contact.contactOrPhoneNumber
                )
            )
            navController.navigate(R.id.contacts_to_single_call_info, bundle)
        }

        //    holder.callType.text = "Incoming" // call.type


        // holder.callTypeIcon.setImageResource(R.drawable.speaker_on)

        // Show or hide missed call indicator
        // holder.missedIndicator.visibility = if (call.missed) View.VISIBLE else View.GONE
        //holder.missedIndicator.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int = contactsListDTO.size
}
*/
