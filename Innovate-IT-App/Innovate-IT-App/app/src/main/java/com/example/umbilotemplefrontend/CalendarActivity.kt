package com.example.umbilotemplefrontend

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CalendarView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.adapters.AgendaAdapter
import com.example.umbilotemplefrontend.adapters.CategorySpinnerAdapter
import com.example.umbilotemplefrontend.adapters.EventsAdapter
import com.example.umbilotemplefrontend.models.Booking
import com.example.umbilotemplefrontend.models.BookingStatus
import com.example.umbilotemplefrontend.models.Event
import com.example.umbilotemplefrontend.models.EventCategory
import com.example.umbilotemplefrontend.utils.BookingRepository
import com.example.umbilotemplefrontend.utils.EventRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.NavigationHandler
import com.example.umbilotemplefrontend.views.CalendarViewMode
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.activity.OnBackPressedCallback

class CalendarActivity : AppCompatActivity() {
    
    private lateinit var calendarView: CalendarView
    private lateinit var agendaView: View
    private lateinit var btnMonthView: Button
    private lateinit var btnAgendaView: Button
    private lateinit var btnToday: Button
    private lateinit var btnClearFilter: Button
    private lateinit var btnRequestBooking: Button
    private lateinit var spinnerCategory: Spinner
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvNoEvents: TextView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var agendaRecyclerView: RecyclerView
    
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var agendaAdapter: AgendaAdapter
    private lateinit var categoryAdapter: CategorySpinnerAdapter
    
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var localDatabase: LocalDatabase
    private lateinit var eventRepository: EventRepository
    private lateinit var bookingRepository: BookingRepository
    
    private val events = mutableListOf<Event>()
    private val filteredEvents = mutableListOf<Event>()
    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private var searchQuery: String = ""
    
    private var currentViewMode = CalendarViewMode.MONTH
    private var selectedDate = Calendar.getInstance().time
    private var selectedCategory: EventCategory? = null
    
    private var requestBooking = false  // Track if opened from bookings page
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_advanced)
        
        try {
            // Set header title
            findViewById<TextView>(R.id.tvHeaderTitle)?.text = "EVENT CALENDAR"
            // Check if opened from bookings for request booking
            requestBooking = intent.getBooleanExtra("REQUEST_BOOKING", false)
            
            // Initialize database and repositories
            localDatabase = LocalDatabase(this)
            eventRepository = EventRepository(this)
            bookingRepository = BookingRepository(this)
            
            // Initialize navigation handler
            navigationHandler = NavigationHandler(this)
            
            // Check if user is logged in
            if (!navigationHandler.checkUserLoggedIn()) {
                return
            }
            
            // Initialize UI components
            initializeViews()
            setupListeners()
            setupAdapters()
            
            // Set up navigation
            navigationHandler.setupNavigation()
            // Ensure bottom navigation is above other content
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)?.bringToFront()
            
            // Load all events
            loadEvents()
            
            // Set initial date and view mode
            updateSelectedDate(Calendar.getInstance().time)
            switchViewMode(CalendarViewMode.MONTH)
            
            // Ensure system back always routes to our handler
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackPressedCompat()
                }
            })
            
        } catch (e: Exception) {
            // Log error and show a message
            android.util.Log.e("CalendarActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error loading calendar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initializeViews() {
        // Calendar views
        calendarView = findViewById(R.id.calendarView)
        agendaView = findViewById(R.id.agendaView)
        
        // Buttons
        btnMonthView = findViewById(R.id.btnMonthView)
        btnAgendaView = findViewById(R.id.btnAgendaView)
        btnToday = findViewById(R.id.btnToday)
        btnClearFilter = findViewById(R.id.btnClearFilter)
        btnRequestBooking = findViewById(R.id.btnRequestBooking)
        
        // Set up back button
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            // Navigate using our explicit back handler
            onBackPressedCompat()
        }
        
        // Other views
        spinnerCategory = findViewById(R.id.spinnerCategory)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvNoEvents = findViewById(R.id.tvNoEvents)
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView)
        
        // Agenda RecyclerView
        agendaRecyclerView = agendaView.findViewById(R.id.agendaRecyclerView)
    }
    
    private fun setupListeners() {
        // View mode buttons
        btnMonthView.setOnClickListener { switchViewMode(CalendarViewMode.MONTH) }
        btnAgendaView.setOnClickListener { switchViewMode(CalendarViewMode.AGENDA) }
        
        // Today button
        btnToday.setOnClickListener {
            updateSelectedDate(Calendar.getInstance().time)
            when (currentViewMode) {
                CalendarViewMode.MONTH -> {
                    calendarView.date = Calendar.getInstance().timeInMillis
                }
                CalendarViewMode.AGENDA -> {
                    // TODO: Implement agenda view navigation to today
                }
            }
        }
        
        // Clear filter button
        btnClearFilter.setOnClickListener {
            spinnerCategory.setSelection(0) // Select "All Categories"
            selectedCategory = null
            filterEvents()
        }

        // Search query listener
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchCalendar)?.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString() ?: ""
                    filterEvents()
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            }
        )
        
        // Request booking button
        btnRequestBooking.setOnClickListener {
            showRequestBookingDialog()
        }
        
        // Calendar date selection
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            updateSelectedDate(calendar.time)
        }
        
        // Category spinner
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val category = categoryAdapter.getItem(position)
                selectedCategory = if (category != null && category.id == "all") null else category
                filterEvents()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategory = null
                filterEvents()
            }
        }
    }
    
    private fun setupAdapters() {
        // Events adapter
        eventsAdapter = EventsAdapter(this, filteredEvents)
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)
        eventsRecyclerView.adapter = eventsAdapter
        
        // Agenda adapter
        agendaAdapter = AgendaAdapter(this, events) { event ->
            showEventDetails(event)
        }
        agendaRecyclerView.layoutManager = LinearLayoutManager(this)
        agendaRecyclerView.adapter = agendaAdapter
        
        // Category adapter
        categoryAdapter = CategorySpinnerAdapter(this, EventCategory.ALL_CATEGORIES)
        spinnerCategory.adapter = categoryAdapter
    }
    
    override fun onResume() {
        super.onResume()
        try {
            // Refresh events in case they were updated elsewhere
            loadEvents()
        } catch (e: Exception) {
            android.util.Log.e("CalendarActivity", "Error in onResume", e)
        }
    }
    
    private fun loadEvents() {
        try {
            // Fetch events from Firebase, fallback to cache
            eventRepository.fetchEventsFromFirestore(
                onSuccess = { savedEvents ->
                    events.clear()
                    events.addAll(savedEvents)
                    
                    // Update adapters
                    filterEvents()
                    agendaAdapter.updateEvents(events)
                },
                onError = { exception ->
                    android.util.Log.e("CalendarActivity", "Error fetching events from Firebase", exception)
                    // Use cached events
                    val savedEvents = eventRepository.getEventsFromCache()
                    events.clear()
                    events.addAll(savedEvents)
                    
                    // Update adapters
                    filterEvents()
                    agendaAdapter.updateEvents(events)
                    
                    Toast.makeText(this, "Using cached events (offline mode)", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("CalendarActivity", "Error loading events", e)
        }
    }
    
    private fun updateSelectedDate(date: Date) {
        selectedDate = date
        tvSelectedDate.text = dateFormat.format(date)
        filterEvents()
    }
    
    private fun filterEvents() {
        try {
            // Create calendar instances for comparison
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.time = selectedDate
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
            selectedCalendar.set(Calendar.MINUTE, 0)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)
            
            val eventCalendar = Calendar.getInstance()
            
            // Filter events for the selected date, category, and search query
            filteredEvents.clear()
            val q = searchQuery.lowercase(Locale.getDefault())
            for (event in events) {
                eventCalendar.time = event.date
                eventCalendar.set(Calendar.HOUR_OF_DAY, 0)
                eventCalendar.set(Calendar.MINUTE, 0)
                eventCalendar.set(Calendar.SECOND, 0)
                eventCalendar.set(Calendar.MILLISECOND, 0)
                
                val dateMatches = selectedCalendar.timeInMillis == eventCalendar.timeInMillis
                val categoryMatches = selectedCategory == null || event.categoryId == selectedCategory?.id
                val textMatches = q.isBlank() ||
                    event.title.lowercase(Locale.getDefault()).contains(q) ||
                    event.description.lowercase(Locale.getDefault()).contains(q) ||
                    event.location.lowercase(Locale.getDefault()).contains(q)
                
                if (dateMatches && categoryMatches && textMatches) {
                    filteredEvents.add(event)
                }
            }
            
            // Update adapter
            eventsAdapter.updateEvents(filteredEvents)
            
            // Show message if no events
            if (filteredEvents.isEmpty()) {
                val categoryText = selectedCategory?.let { " in category ${it.name}" } ?: ""
                tvSelectedDate.text = "${dateFormat.format(selectedDate)} - No events$categoryText"
                tvNoEvents.visibility = View.VISIBLE
            } else {
                val categoryText = selectedCategory?.let { " in category ${it.name}" } ?: ""
                tvSelectedDate.text = "${dateFormat.format(selectedDate)} - ${filteredEvents.size} event(s)$categoryText"
                tvNoEvents.visibility = View.GONE
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarActivity", "Error filtering events", e)
            tvNoEvents.visibility = View.VISIBLE
            tvNoEvents.text = "Error filtering events: ${e.message}"
        }
    }
    
    private fun switchViewMode(mode: CalendarViewMode) {
        currentViewMode = mode
        
        // Update button styles
        btnMonthView.isEnabled = mode != CalendarViewMode.MONTH
        btnAgendaView.isEnabled = mode != CalendarViewMode.AGENDA
        
        // Show/hide views based on mode
        when (mode) {
            CalendarViewMode.MONTH -> {
                calendarView.visibility = View.VISIBLE
                agendaView.visibility = View.GONE
                eventsRecyclerView.visibility = View.VISIBLE
            }
            CalendarViewMode.AGENDA -> {
                calendarView.visibility = View.GONE
                agendaView.visibility = View.VISIBLE
                eventsRecyclerView.visibility = View.GONE
                
                // Update agenda view
                agendaAdapter.updateEvents(events)
            }
        }
    }
    
    private fun showEventDetails(event: Event) {
        val message = """
            Title: ${event.title}
            Date: ${dateFormat.format(event.date)}
            Time: ${event.time}
            Location: ${event.location}
            Category: ${event.getCategory().name}
            
            ${event.description}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Event Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun showRequestBookingDialog() {
        val currentUser = localDatabase.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to request a booking", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = android.view.LayoutInflater.from(this)
            .inflate(R.layout.dialog_request_booking, null)
        
        val tvSelectedDateDisplay = dialogView.findViewById<TextView>(R.id.tvSelectedDateDisplay)
        val etEventName = dialogView.findViewById<TextInputEditText>(R.id.etEventName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val etTime = dialogView.findViewById<TextInputEditText>(R.id.etTime)
        val etAttendees = dialogView.findViewById<TextInputEditText>(R.id.etAttendees)
        val etContactNumber = dialogView.findViewById<TextInputEditText>(R.id.etContactNumber)
        
        // Display the selected date
        tvSelectedDateDisplay.text = "Date: ${dateFormat.format(selectedDate)}"
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val eventName = etEventName.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val time = etTime.text.toString().trim()
            val attendeesStr = etAttendees.text.toString().trim()
            val contactNumber = etContactNumber.text.toString().trim()
            
            // Validate inputs
            if (eventName.isEmpty() || description.isEmpty() || time.isEmpty() || 
                attendeesStr.isEmpty() || contactNumber.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val attendees = attendeesStr.toIntOrNull()
            if (attendees == null || attendees <= 0) {
                Toast.makeText(this, "Please enter a valid number of attendees", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if selected date is in the past
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            
            val selectedCal = Calendar.getInstance()
            selectedCal.time = selectedDate
            selectedCal.set(Calendar.HOUR_OF_DAY, 0)
            selectedCal.set(Calendar.MINUTE, 0)
            selectedCal.set(Calendar.SECOND, 0)
            selectedCal.set(Calendar.MILLISECOND, 0)
            
            if (selectedCal.before(today)) {
                Toast.makeText(this, "Cannot book dates in the past", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Create booking request
            val booking = Booking(
                id = UUID.randomUUID().toString(),
                userId = currentUser.id,
                userName = currentUser.name,
                userEmail = currentUser.email,
                eventName = eventName,
                description = description,
                requestedDate = selectedDate,
                requestedTime = time,
                estimatedAttendees = attendees,
                contactNumber = contactNumber,
                status = BookingStatus.PENDING,
                submittedDate = Date()
            )
            
            // Submit booking
            submitBookingRequest(booking)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun submitBookingRequest(booking: Booking) {
        bookingRepository.saveBookingToFirestore(
            booking,
            onSuccess = {
                Toast.makeText(
                    this,
                    "Booking request submitted successfully!",
                    Toast.LENGTH_LONG
                ).show()
                
                // Show confirmation dialog
                AlertDialog.Builder(this)
                    .setTitle("Booking Submitted")
                    .setMessage("Your booking request for '${booking.eventName}' has been submitted for review. You will be notified once the admin reviews your request.\n\nYou can check the status in the Bookings tab.")
                    .setPositiveButton("OK") { _, _ ->
                        // Return to bookings page if opened from there
                        if (requestBooking) {
                            finish()
                        }
                    }
                    .setNeutralButton("View Bookings") { _, _ ->
                        startActivity(android.content.Intent(this, BookingsActivity::class.java))
                        finish()
                    }
                    .show()
            },
            onError = { exception ->
                Toast.makeText(
                    this,
                    "Error submitting booking: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
    
    private fun onBackPressedCompat() {
        // If opened from bookings page, go back to bookings
        if (requestBooking) {
            finish()
        } else {
            // Otherwise go to home
            val intent = android.content.Intent(this, HomeActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    override fun onBackPressed() {
        onBackPressedCompat()
    }
}