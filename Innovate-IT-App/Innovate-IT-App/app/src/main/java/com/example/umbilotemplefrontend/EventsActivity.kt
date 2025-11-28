package com.example.umbilotemplefrontend

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.example.umbilotemplefrontend.adapters.CategorySpinnerAdapter
import com.example.umbilotemplefrontend.models.EventCategory
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.adapters.EventsAdapter
import com.example.umbilotemplefrontend.models.Event
import com.example.umbilotemplefrontend.utils.EventRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.NavigationHandler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class EventsActivity : AppCompatActivity(), EventsAdapter.EventActionListener {
    
    private lateinit var localDatabase: LocalDatabase
    private lateinit var eventRepository: EventRepository
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var navigationHandler: NavigationHandler
    private val events = mutableListOf<Event>()
    private var filtered: List<Event> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)
        
        // Initialize local database and repository
        localDatabase = LocalDatabase(this)
        eventRepository = EventRepository(this)
        
        // Initialize navigation handler
        navigationHandler = NavigationHandler(this)
        
        // Check if user is logged in
        if (!navigationHandler.checkUserLoggedIn()) {
            return
        }
        
        // Set header title and back button
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "EVENTS"
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            // Navigate to home explicitly
            navigateHome()
        }
        
        // Set up RecyclerView
        val eventsRecyclerView = findViewById<RecyclerView>(R.id.eventsRecyclerView)
        
        // Check if user is admin
        val currentUser = localDatabase.getCurrentUser()
        val isAdmin = currentUser?.isAdmin == true
        
        // Only show delete option to admins
        eventsAdapter = EventsAdapter(this, events, if (isAdmin) this else null)
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)
        eventsRecyclerView.adapter = eventsAdapter
        
        // Load events
        loadEvents()
        
        // Search filtering
        val etSearch = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchEvents)
        etSearch?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyEventsFilter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Set up navigation
        navigationHandler.setupNavigation()
		
		// Handle system back press consistently
		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				navigateHome()
			}
		})
    }

    override fun onStart() {
        super.onStart()
        // Real-time events updates to reflect changes immediately
        try {
            eventRepository.startRealtimeListener(
                onEventsChanged = { updated ->
                    events.clear()
                    events.addAll(updated)
                    applyEventsFilter(findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchEvents)?.text?.toString() ?: "")
                },
                onError = { _ -> }
            )
        } catch (_: Exception) { }
    }

    override fun onStop() {
        super.onStop()
        try { eventRepository.stopRealtimeListener() } catch (_: Exception) { }
    }
    
    override fun onBackPressed() {
        // Always navigate to Home instead of exiting the app
        navigateHome()
    }

    private fun navigateHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun loadEvents() {
        // Fetch events from Firebase, fallback to cache
        eventRepository.fetchEventsFromFirestore(
            onSuccess = { savedEvents ->
                events.clear()
                events.addAll(savedEvents)
                applyEventsFilter(findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchEvents)?.text?.toString() ?: "")
            },
            onError = { exception ->
                // Use cached events
                val savedEvents = eventRepository.getEventsFromCache()
                events.clear()
                events.addAll(savedEvents)
                applyEventsFilter(findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchEvents)?.text?.toString() ?: "")
                Toast.makeText(this, "Using cached events (offline mode)", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applyEventsFilter(query: String) {
        if (query.isBlank()) {
            filtered = events
        } else {
            val q = query.lowercase(Locale.getDefault())
            filtered = events.filter { event ->
                event.title.lowercase(Locale.getDefault()).contains(q) ||
                event.description.lowercase(Locale.getDefault()).contains(q) ||
                event.location.lowercase(Locale.getDefault()).contains(q)
            }
        }
        eventsAdapter.updateEvents(filtered)
    }
    
    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etEventTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etEventDescription)
        val etLocation = dialogView.findViewById<EditText>(R.id.etEventLocation)
        val etRequirements = dialogView.findViewById<EditText>(R.id.etEventRequirements)
        val etDressCode = dialogView.findViewById<EditText>(R.id.etEventDressCode)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val cbIsFirewalking = dialogView.findViewById<CheckBox>(R.id.cbIsFirewalking)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        
        // Set up category spinner
        val categoryAdapter = CategorySpinnerAdapter(this, EventCategory.ALL_CATEGORIES)
        spinnerCategory.adapter = categoryAdapter
        
        // Set default selection to Religious for temple events
        for (i in 0 until categoryAdapter.count) {
            val category = categoryAdapter.getItem(i)
            if (category != null && category.id == EventCategory.RELIGIOUS.id) {
                spinnerCategory.setSelection(i)
                break
            }
        }
        
        val calendar = Calendar.getInstance()
        var selectedDate = calendar.time
        var selectedTime = "12:00 PM"
        
        btnSelectDate.text = dateFormat.format(selectedDate)
        btnSelectTime.text = selectedTime
        
        btnSelectDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    btnSelectDate.text = dateFormat.format(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
        
        btnSelectTime.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    selectedTime = timeFormat.format(calendar.time)
                    btnSelectTime.text = selectedTime
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            )
            timePickerDialog.show()
        }
        
        // Set up firewalking checkbox to automatically select Firewalking category
        cbIsFirewalking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                for (i in 0 until categoryAdapter.count) {
                    val category = categoryAdapter.getItem(i)
                    if (category != null && category.id == EventCategory.FIREWALKING.id) {
                        spinnerCategory.setSelection(i)
                        break
                    }
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Add New Event")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val requirements = etRequirements.text.toString().trim()
                val dressCode = etDressCode.text.toString().trim()
                val isFirewalking = cbIsFirewalking.isChecked
                
                if (title.isEmpty()) {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Get selected category
                val selectedCategory = spinnerCategory.selectedItem as? EventCategory
                val categoryId = selectedCategory?.id ?: EventCategory.DEFAULT.id
                
                val newEvent = Event(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    date = selectedDate,
                    time = selectedTime,
                    location = location,
                    requirements = requirements,
                    dresscode = dressCode,
                    isFirewalking = isFirewalking,
                    categoryId = categoryId
                )
                
                // Save to Firebase
                eventRepository.saveEventToFirestore(
                    newEvent,
                    onSuccess = {
                        Toast.makeText(this, "Event added successfully", Toast.LENGTH_SHORT).show()
                        loadEvents() // Reload to refresh the list
                    },
                    onError = { exception ->
                        Toast.makeText(this, "Error adding event: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDeleteEvent(position: Int) {
        val event = events[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Yes") { _, _ ->
                // Delete from Firebase
                eventRepository.deleteEventFromFirestore(
                    event.id,
                    onSuccess = {
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show()
                        loadEvents() // Reload to refresh the list
                    },
                    onError = { exception ->
                        Toast.makeText(this, "Error deleting event: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("No", null)
            .show()
    }
} 