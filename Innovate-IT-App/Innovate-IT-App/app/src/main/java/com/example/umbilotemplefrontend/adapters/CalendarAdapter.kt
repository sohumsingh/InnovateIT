package com.example.umbilotemplefrontend.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.umbilotemplefrontend.R
import androidx.core.content.ContextCompat
import com.example.umbilotemplefrontend.models.Event
import java.util.Calendar
import java.util.Date
import java.util.HashSet

class CalendarAdapter(
    context: Context,
    private val monthYear: Date,
    private val events: List<Event>
) : ArrayAdapter<Date>(context, R.layout.item_calendar_day) {
    
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val currentDate = Calendar.getInstance()
    private val dates = ArrayList<Date>()
    private val eventDates = HashSet<Int>()
    
    init {
        // Initialize calendar with the provided month/year
        val calendar = Calendar.getInstance()
        calendar.time = monthYear
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        
        // Calculate first day of month and required grid size
        val monthBeginningCell = calendar.get(Calendar.DAY_OF_WEEK) - 1
        calendar.add(Calendar.DAY_OF_MONTH, -monthBeginningCell)
        
        // Fill dates for grid cells
        while (dates.size < DAYS_COUNT) {
            dates.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Mark dates with events
        for (event in events) {
            val eventCalendar = Calendar.getInstance()
            eventCalendar.time = event.date
            
            // Only consider events in the current month/year
            if (isSameMonthYear(eventCalendar, monthYear)) {
                eventDates.add(eventCalendar.get(Calendar.DAY_OF_MONTH))
            }
        }
    }
    
    override fun getCount(): Int {
        return dates.size
    }
    
    override fun getItem(position: Int): Date? {
        return dates[position]
    }
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_calendar_day, parent, false)
        val dayText = view.findViewById<TextView>(R.id.tvCalendarDay)
        val eventIndicator = view.findViewById<View>(R.id.viewEventIndicator)
        
        // Get date for this position
        val calendar = Calendar.getInstance()
        calendar.time = dates[position]
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Set day number
        dayText.text = day.toString()
        
        // Style for days from a different month
        if (!isSameMonthYear(calendar, monthYear)) {
            val disabledColor = ContextCompat.getColor(context, R.color.text_secondary_on_bg)
            dayText.setTextColor(disabledColor)
        } else {
            val enabledColor = ContextCompat.getColor(context, R.color.text_on_bg)
            dayText.setTextColor(enabledColor)
            
            // Highlight today
            if (calendar.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == currentDate.get(Calendar.DAY_OF_MONTH)
            ) {
                dayText.setTypeface(null, Typeface.BOLD)
                dayText.setBackgroundResource(R.drawable.calendar_today_background)
            }
            
            // Show event indicator
            if (eventDates.contains(day)) {
                eventIndicator.visibility = View.VISIBLE
            } else {
                eventIndicator.visibility = View.INVISIBLE
            }
        }
        
        return view
    }
    
    private fun isSameMonthYear(cal1: Calendar, date2: Date): Boolean {
        val cal2 = Calendar.getInstance()
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
    
    companion object {
        private const val DAYS_COUNT = 42 // 6 rows, 7 columns
    }
}
