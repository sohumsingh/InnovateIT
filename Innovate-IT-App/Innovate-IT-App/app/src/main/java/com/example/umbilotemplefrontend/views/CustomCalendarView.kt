package com.example.umbilotemplefrontend.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.adapters.CalendarAdapter
import com.example.umbilotemplefrontend.models.Event
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CustomCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val calendar = Calendar.getInstance()
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var calendarGrid: GridView
    
    private var events: List<Event> = emptyList()
    private var onDateSelectedListener: ((Date) -> Unit)? = null
    
    init {
        orientation = VERTICAL
        
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.custom_calendar_view, this, true)
        
        // Initialize views
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        calendarGrid = findViewById(R.id.calendarGrid)
        
        // Set up navigation buttons
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }
        
        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
        
        // Set up grid click listener
        calendarGrid.setOnItemClickListener { _, _, position, _ ->
            val adapter = calendarGrid.adapter as CalendarAdapter
            val selectedDate = adapter.getItem(position)
            selectedDate?.let { onDateSelectedListener?.invoke(it) }
        }
        
        // Initial update
        updateCalendar()
    }
    
    fun updateCalendar(events: List<Event> = this.events) {
        this.events = events
        
        // Update header
        tvMonthYear.text = monthYearFormat.format(calendar.time)
        
        // Update grid
        val adapter = CalendarAdapter(context, calendar.time, events)
        calendarGrid.adapter = adapter
    }
    
    fun setOnDateSelectedListener(listener: (Date) -> Unit) {
        onDateSelectedListener = listener
    }
    
    fun goToToday() {
        calendar.time = Date()
        updateCalendar()
    }
    
    fun goToMonth(year: Int, month: Int) {
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        updateCalendar()
    }
    
    fun getCurrentMonth(): Date {
        return calendar.time
    }
}
