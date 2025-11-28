package com.example.umbilotemplefrontend.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.EventCategory

class CategorySpinnerAdapter(
    context: Context,
    categories: List<EventCategory>
) : ArrayAdapter<EventCategory>(context, android.R.layout.simple_spinner_item, categories) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)
        
        val category = getItem(position)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        
        if (category != null) {
            textView.text = category.name
            textView.setTextColor(category.color)
        }
        
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        
        val category = getItem(position)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        
        if (category != null) {
            textView.text = category.name
            textView.setTextColor(category.color)
        }
        
        return view
    }
}