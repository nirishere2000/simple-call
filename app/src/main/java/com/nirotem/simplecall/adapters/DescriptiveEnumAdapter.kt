package com.nirotem.simplecall.adapters

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.StringRes
import interfaces.DescriptiveEnum

class DescriptiveEnumAdapter<T>(
    context: Context,
    private val enums: List<T>
) : ArrayAdapter<T>(context, android.R.layout.simple_spinner_item, enums) where T : DescriptiveEnum, T : Enum<T> {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.setTextColor(Color.BLACK) // Customize as needed
        textView.text = context.getString(enums[position].descriptionRes)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.setBackgroundColor(Color.WHITE) // Customize as needed
        textView.setTextColor(Color.BLACK) // Customize as needed
        textView.text = context.getString(enums[position].descriptionRes)
        return view
    }
}
