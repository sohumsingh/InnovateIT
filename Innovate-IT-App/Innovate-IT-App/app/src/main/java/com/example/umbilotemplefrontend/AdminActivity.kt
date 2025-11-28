package com.example.umbilotemplefrontend

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.adapters.AdminBookingsAdapter
import com.example.umbilotemplefrontend.adapters.AdminDocumentsAdapter
import com.example.umbilotemplefrontend.adapters.AdminGalleryAdapter
import com.example.umbilotemplefrontend.models.MediaStatus
import com.example.umbilotemplefrontend.adapters.BankAccountsAdapter
import com.example.umbilotemplefrontend.adapters.CategorySpinnerAdapter
import com.example.umbilotemplefrontend.adapters.EventsAdminAdapter
import com.example.umbilotemplefrontend.adapters.UsersAdapter
import com.example.umbilotemplefrontend.models.BankAccount
import com.example.umbilotemplefrontend.models.Booking
import com.example.umbilotemplefrontend.models.BookingStatus
import com.example.umbilotemplefrontend.models.Document
import com.example.umbilotemplefrontend.models.EventCategory
import com.example.umbilotemplefrontend.models.Event
import com.example.umbilotemplefrontend.models.User
import com.example.umbilotemplefrontend.utils.BankAccountRepository
import com.example.umbilotemplefrontend.utils.BookingRepository
import com.example.umbilotemplefrontend.utils.DocumentRepository
import com.example.umbilotemplefrontend.utils.EventRepository
import com.example.umbilotemplefrontend.utils.UserRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * AdminActivity provides functionality for admin users to manage users and events
 */
enum class DocumentsViewMode {
    USER_LIST,      // Show list of users to select from
    USER_DOCUMENTS  // Show selected user's documents
}

class AdminActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerViewUsers: RecyclerView
    private lateinit var recyclerViewEvents: RecyclerView
    private lateinit var recyclerViewBanking: RecyclerView
    private lateinit var recyclerViewBookings: RecyclerView
    private lateinit var recyclerViewDocuments: RecyclerView
    private lateinit var recyclerViewGallery: RecyclerView
    private lateinit var btnAddEvent: Button
    private lateinit var btnAddBankAccount: Button
    private lateinit var fabMain: FloatingActionButton
    private lateinit var tvNoUsers: TextView
    private lateinit var tvNoEvents: TextView
    private lateinit var tvNoBanking: TextView
    private lateinit var tvNoBookings: TextView
    private lateinit var tvNoDocuments: TextView
    private lateinit var tvNoGallery: TextView
    private lateinit var etSearchUsers: TextInputEditText
    private lateinit var etSearchDocuments: TextInputEditText
    private var allUsers: List<User> = emptyList()
    private var allDocuments: List<Document> = emptyList()
    private var selectedUserEmail: String? = null
    private var documentsViewMode: DocumentsViewMode = DocumentsViewMode.USER_LIST
    private var isProgrammaticSearchChange: Boolean = false
    private lateinit var localDatabase: LocalDatabase
    private lateinit var bankAccountRepository: BankAccountRepository
    private lateinit var bookingRepository: BookingRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var userRepository: UserRepository
    private lateinit var documentRepository: DocumentRepository
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var eventsAdapter: EventsAdminAdapter
    private lateinit var bankAccountsAdapter: BankAccountsAdapter
    private lateinit var bookingsAdapter: AdminBookingsAdapter
    private lateinit var documentsAdapter: AdminDocumentsAdapter
    private lateinit var galleryAdapter: AdminGalleryAdapter
    private var currentUser: User? = null
    private var dataLoaded = false  // Track if data has finished loading
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_admin_new)
            
            // Initialize database and repositories
            localDatabase = LocalDatabase(this)
            bankAccountRepository = BankAccountRepository(this)
            bookingRepository = BookingRepository(this)
            eventRepository = EventRepository(this)
            userRepository = UserRepository(this)
            documentRepository = DocumentRepository(this)
            
            // Get current user
            var user = localDatabase.getCurrentUser()
            
            // CRITICAL FIX: Ensure admin email always has admin privileges
            if (user != null && user.email.equals("TestingAdmin@gmail.com", ignoreCase = true) && !user.isAdmin) {
                Log.w("AdminActivity", "Admin user lost privileges! Restoring...")
                user = user.copy(isAdmin = true)
                localDatabase.updateUser(user)
                localDatabase.setCurrentUser(user)
                Log.d("AdminActivity", "Admin privileges restored!")
            }
            
            // Update the class property
            currentUser = user
            
            // Check if user is admin
            if (currentUser?.isAdmin != true) {
                Toast.makeText(this, "You don't have admin privileges", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Initialize views
            tabLayout = findViewById(R.id.tabLayout)
            // Set header title via include
            findViewById<TextView>(R.id.tvHeaderTitle)?.text = "ADMIN PANEL"
            recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
            recyclerViewEvents = findViewById(R.id.recyclerViewEvents)
            recyclerViewBanking = findViewById(R.id.recyclerViewBanking)
            recyclerViewBookings = findViewById(R.id.recyclerViewBookings)
            recyclerViewDocuments = findViewById(R.id.recyclerViewDocuments)
            recyclerViewGallery = findViewById(R.id.recyclerViewGallery)
            btnAddEvent = findViewById(R.id.btnAddEvent)
            btnAddBankAccount = findViewById(R.id.btnAddBankAccount)
            fabMain = findViewById(R.id.fabMain)
            tvNoUsers = findViewById(R.id.tvNoUsers)
            tvNoEvents = findViewById(R.id.tvNoEvents)
            tvNoBanking = findViewById(R.id.tvNoBanking)
            tvNoBookings = findViewById(R.id.tvNoBookings)
            tvNoDocuments = findViewById(R.id.tvNoDocuments)
            tvNoGallery = findViewById(R.id.tvNoGallery)
            etSearchUsers = findViewById(R.id.etSearchUsers)
            etSearchDocuments = findViewById(R.id.etSearchDocuments)
            
            // Set up recycler views
            recyclerViewUsers.layoutManager = LinearLayoutManager(this)
            recyclerViewEvents.layoutManager = LinearLayoutManager(this)
            recyclerViewBanking.layoutManager = LinearLayoutManager(this)
            recyclerViewBookings.layoutManager = LinearLayoutManager(this)
            recyclerViewDocuments.layoutManager = LinearLayoutManager(this)
            recyclerViewGallery.layoutManager = LinearLayoutManager(this)
            
            // Set up tab selection listener
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    updateVisibility(tab?.position ?: 0)
                }
                
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            
            // Set up add event button
            btnAddEvent.setOnClickListener {
                showAddEventDialog()
            }
            
            // Set up add bank account button
            btnAddBankAccount.setOnClickListener {
                showAddBankAccountDialog(null)
            }
            
            // Set up FAB for quick actions
            fabMain.setOnClickListener {
                handleFABClick()
            }
            
            // Set up back button
            try {
                findViewById<android.widget.ImageButton>(R.id.btnBack)?.setOnClickListener {
                    // CRITICAL: Ensure admin status is preserved before going back
                    val currentAdminUser = localDatabase.getCurrentUser()
                    if (currentAdminUser != null && currentAdminUser.email.equals("TestingAdmin@gmail.com", ignoreCase = true) && !currentAdminUser.isAdmin) {
                        Log.w("AdminActivity", "Restoring admin status before going back")
                        val fixedUser = currentAdminUser.copy(isAdmin = true)
                        localDatabase.updateUser(fixedUser)
                        localDatabase.setCurrentUser(fixedUser)
                    }
                    
                    // Navigate to home page - use finish() to go back instead of creating new activity
                    finish()
                }
            } catch (e: Exception) {
                Log.d("AdminActivity", "Back button not found in layout")
            }
            
            // Set up search functionality
            etSearchUsers.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterUsers(s?.toString() ?: "")
                }
            })
            
            etSearchDocuments.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    // Check if search is cleared and we're viewing documents
                    if (s.isNullOrEmpty() && documentsViewMode == DocumentsViewMode.USER_DOCUMENTS && !isProgrammaticSearchChange) {
                        // User explicitly cleared search - go back to user list
                        showAllUsersForDocumentsTab()
                    }
                    isProgrammaticSearchChange = false
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!isProgrammaticSearchChange) {
                        if (documentsViewMode == DocumentsViewMode.USER_LIST) {
                            filterDocumentsUsers(s?.toString() ?: "")
                        } else {
                            filterUserDocuments(s?.toString() ?: "")
                        }
                    }
                }
            })
            
            // Load data
            loadUsers()
            loadEvents()
            loadBankAccounts()
            loadBookings()
            loadDocuments()
            loadGallery()
            
            // Hide all "No X found" messages initially to prevent overlap
            tvNoUsers.visibility = View.GONE
            tvNoEvents.visibility = View.GONE
            tvNoBanking.visibility = View.GONE
            tvNoBookings.visibility = View.GONE
            tvNoDocuments.visibility = View.GONE
            tvNoGallery.visibility = View.GONE
            
            // Set initial visibility
            updateVisibility(0)
            
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            // Real-time sync for documents so approvals/deletes reflect immediately
            documentRepository.startRealtimeListener(
                onDocumentsChanged = { docs ->
                    allDocuments = docs
                    if (documentsViewMode == DocumentsViewMode.USER_DOCUMENTS && selectedUserEmail != null) {
                        val filtered = docs.filter { it.uploadedByEmail == selectedUserEmail }
                        if (::recyclerViewDocuments.isInitialized && recyclerViewDocuments.adapter is AdminDocumentsAdapter) {
                            (recyclerViewDocuments.adapter as AdminDocumentsAdapter).updateDocuments(filtered)
                            tvNoDocuments.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                },
                onError = { _ -> }
            )
        } catch (_: Exception) { }
    }

    override fun onStop() {
        super.onStop()
        try {
            documentRepository.stopRealtimeListener()
        } catch (_: Exception) { }
    }
    
    private fun updateVisibility(tabPosition: Int) {
        // Find and control search field visibility
        val searchUsersLayout = findViewById<View>(R.id.searchUsersLayout)
        
        when (tabPosition) {
            0 -> { // Users tab
                if (searchUsersLayout != null) searchUsersLayout.visibility = View.VISIBLE
                recyclerViewUsers.visibility = View.VISIBLE
                // Only show "No users" if data has loaded and adapter is empty
                tvNoUsers.visibility = if (dataLoaded && ::usersAdapter.isInitialized && usersAdapter.itemCount == 0) View.VISIBLE else View.GONE
                recyclerViewEvents.visibility = View.GONE
                tvNoEvents.visibility = View.GONE
                recyclerViewBanking.visibility = View.GONE
                tvNoBanking.visibility = View.GONE
                recyclerViewBookings.visibility = View.GONE
                tvNoBookings.visibility = View.GONE
                recyclerViewDocuments.visibility = View.GONE
                tvNoDocuments.visibility = View.GONE
                recyclerViewGallery.visibility = View.GONE
                tvNoGallery.visibility = View.GONE
                btnAddEvent.visibility = View.GONE
                btnAddBankAccount.visibility = View.GONE
            }
            1 -> { // Events tab
                if (searchUsersLayout != null) searchUsersLayout.visibility = View.GONE
                recyclerViewUsers.visibility = View.GONE
                tvNoUsers.visibility = View.GONE
                recyclerViewEvents.visibility = View.VISIBLE
                tvNoEvents.visibility = if (dataLoaded && ::eventsAdapter.isInitialized && eventsAdapter.itemCount == 0) View.VISIBLE else View.GONE
                recyclerViewBanking.visibility = View.GONE
                tvNoBanking.visibility = View.GONE
                recyclerViewBookings.visibility = View.GONE
                tvNoBookings.visibility = View.GONE
                recyclerViewDocuments.visibility = View.GONE
                tvNoDocuments.visibility = View.GONE
                recyclerViewGallery.visibility = View.GONE
                tvNoGallery.visibility = View.GONE
                btnAddEvent.visibility = View.VISIBLE
                btnAddBankAccount.visibility = View.GONE
            }
            2 -> { // Banking tab
                if (searchUsersLayout != null) searchUsersLayout.visibility = View.GONE
                recyclerViewUsers.visibility = View.GONE
                tvNoUsers.visibility = View.GONE
                recyclerViewEvents.visibility = View.GONE
                tvNoEvents.visibility = View.GONE
                recyclerViewBanking.visibility = View.VISIBLE
                tvNoBanking.visibility = if (dataLoaded && ::bankAccountsAdapter.isInitialized && bankAccountsAdapter.itemCount == 0) View.VISIBLE else View.GONE
                recyclerViewBookings.visibility = View.GONE
                tvNoBookings.visibility = View.GONE
                recyclerViewDocuments.visibility = View.GONE
                tvNoDocuments.visibility = View.GONE
                recyclerViewGallery.visibility = View.GONE
                tvNoGallery.visibility = View.GONE
                btnAddEvent.visibility = View.GONE
                btnAddBankAccount.visibility = View.VISIBLE
            }
            3 -> { // Bookings tab
                if (searchUsersLayout != null) searchUsersLayout.visibility = View.GONE
                recyclerViewUsers.visibility = View.GONE
                tvNoUsers.visibility = View.GONE
                recyclerViewEvents.visibility = View.GONE
                tvNoEvents.visibility = View.GONE
                recyclerViewBanking.visibility = View.GONE
                tvNoBanking.visibility = View.GONE
                recyclerViewBookings.visibility = View.VISIBLE
                tvNoBookings.visibility = if (dataLoaded && ::bookingsAdapter.isInitialized && bookingsAdapter.itemCount == 0) View.VISIBLE else View.GONE
                recyclerViewDocuments.visibility = View.GONE
                tvNoDocuments.visibility = View.GONE
                recyclerViewGallery.visibility = View.GONE
                tvNoGallery.visibility = View.GONE
                btnAddEvent.visibility = View.GONE
                btnAddBankAccount.visibility = View.GONE
            }
            4 -> { // Gallery tab
                if (searchUsersLayout != null) searchUsersLayout.visibility = View.GONE
                recyclerViewUsers.visibility = View.GONE
                tvNoUsers.visibility = View.GONE
                recyclerViewEvents.visibility = View.GONE
                tvNoEvents.visibility = View.GONE
                recyclerViewBanking.visibility = View.GONE
                tvNoBanking.visibility = View.GONE
                recyclerViewBookings.visibility = View.GONE
                tvNoBookings.visibility = View.GONE
                recyclerViewGallery.visibility = View.VISIBLE
                tvNoGallery.visibility = if (dataLoaded && ::galleryAdapter.isInitialized && galleryAdapter.itemCount == 0) View.VISIBLE else View.GONE
                btnAddEvent.visibility = View.GONE
                btnAddBankAccount.visibility = View.GONE
            }
        }
    }
    
    private fun loadUsers() {
        try {
            // Fetch users from Firebase, fallback to cache
            userRepository.fetchUsersFromFirestore(
                onSuccess = { allUsers ->
                    val users = allUsers.filter { it.id != currentUser?.id }
                    displayUsers(users)
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error fetching users from Firebase", exception)
                    // Use cached users
                    val allUsers = userRepository.getUsersFromCache()
                    val users = allUsers.filter { it.id != currentUser?.id }
                    displayUsers(users)
                    Toast.makeText(this, "Using cached users (offline mode)", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error loading users", e)
            Toast.makeText(this, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayUsers(users: List<User>) {
        allUsers = users  // Store all users for filtering
        
        // Set up adapter
        usersAdapter = UsersAdapter(this, users, object : UsersAdapter.UserActionListener {
            override fun onToggleAdminStatus(user: User, position: Int) {
                toggleAdminStatus(user, position)
            }
            
            override fun onDeleteUser(user: User, position: Int) {
                confirmDeleteUser(user, position)
            }
            
            override fun onViewUserDocuments(user: User, position: Int) {
                viewUserDocuments(user)
            }
        })
        
        recyclerViewUsers.adapter = usersAdapter
        
        // Mark data as loaded
        dataLoaded = true
        
        // Update empty view only if we're on the Users tab
        if (tabLayout.selectedTabPosition == 0) {
            tvNoUsers.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun toggleAdminStatus(user: User, position: Int) {
        try {
            // Toggle admin status
            val updatedUser = user.copy(isAdmin = !user.isAdmin)
            
            // Update in Firebase
            userRepository.updateUserInFirestore(
                updatedUser,
                onSuccess = {
                    // Reload users to refresh the list
                    loadUsers()
                    
                    // Show success message
                    val message = if (updatedUser.isAdmin) {
                        "${user.name} is now an admin"
                    } else {
                        "${user.name} is no longer an admin"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error updating user in Firebase", exception)
                    Toast.makeText(this, "Failed to update user: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error toggling admin status", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun confirmDeleteUser(user: User, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(user, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteUser(user: User, position: Int) {
        try {
            // Delete user from Firebase
            userRepository.deleteUserFromFirestore(
                user.id,
                onSuccess = {
                    Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
                    // Reload users to refresh the list
                    loadUsers()
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error deleting user from Firebase", exception)
                    Toast.makeText(this, "Failed to delete user: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error deleting user", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadEvents() {
        try {
            // Fetch events from Firebase, fallback to cache
            eventRepository.fetchEventsFromFirestore(
                onSuccess = { events ->
                    displayEvents(events)
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error fetching events from Firebase", exception)
                    // Use cached events
                    val cachedEvents = eventRepository.getEventsFromCache()
                    displayEvents(cachedEvents)
                    Toast.makeText(this, "Using cached events (offline mode)", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error loading events", e)
            Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayEvents(events: List<Event>) {
        // Set up adapter
        eventsAdapter = EventsAdminAdapter(this, events, object : EventsAdminAdapter.EventActionListener {
            override fun onDeleteEvent(event: Event, position: Int) {
                confirmDeleteEvent(event, position)
            }
            
            override fun onViewEventDetails(event: Event, position: Int) {
                showEventDetailsDialog(event)
            }
        })
        
        recyclerViewEvents.adapter = eventsAdapter
        
        // Mark data as loaded
        if (!dataLoaded) dataLoaded = true
        
        // Update empty view only if we're on the Events tab
        if (tabLayout.selectedTabPosition == 1) {
            tvNoEvents.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun showAddEventDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null)
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
                
                // Create new event
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
                
                addEvent(newEvent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addEvent(event: Event) {
        try {
            // Save to Firebase
            eventRepository.saveEventToFirestore(
                event,
                onSuccess = {
                    Toast.makeText(this, "Event added successfully", Toast.LENGTH_SHORT).show()
                    // Reload events to refresh the list
                    loadEvents()
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error saving event to Firebase", exception)
                    Toast.makeText(this, "Error adding event: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error adding event", e)
            Toast.makeText(this, "Error adding event: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun confirmDeleteEvent(event: Event, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete ${event.title}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(event, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteEvent(event: Event, position: Int) {
        try {
            // Delete from Firebase
            eventRepository.deleteEventFromFirestore(
                event.id,
                onSuccess = {
                    Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                    // Reload events to refresh the list
                    loadEvents()
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error deleting event from Firebase", exception)
                    Toast.makeText(this, "Error deleting event: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error deleting event", e)
            Toast.makeText(this, "Error deleting event: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showEventDetailsDialog(event: Event) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event_details, null)
        
        dialogView.findViewById<TextView>(R.id.tvEventTitleDetail).text = event.title
        dialogView.findViewById<TextView>(R.id.tvEventDateTimeDetail).text = 
            "${dateFormat.format(event.date)} at ${event.time}"
        dialogView.findViewById<TextView>(R.id.tvEventLocationDetail).text = event.location
        dialogView.findViewById<TextView>(R.id.tvEventDescriptionDetail).text = event.description
        dialogView.findViewById<TextView>(R.id.tvEventRequirementsDetail).text = 
            if (event.requirements.isNotEmpty()) event.requirements else "None"
        dialogView.findViewById<TextView>(R.id.tvEventDressCodeDetail).text = 
            if (event.dresscode.isNotEmpty()) event.dresscode else "None"
        dialogView.findViewById<TextView>(R.id.tvEventFirewalkingDetail).text = 
            "Firewalking: ${if (event.isFirewalking) "Yes" else "No"}"
        
        AlertDialog.Builder(this)
            .setTitle("Event Details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }
    
    // ==================== Banking Management Methods ====================
    
    private fun loadBankAccounts() {
        try {
            // Fetch from Firestore, fall back to local cache
            bankAccountRepository.fetchBankAccountsFromFirestore(
                onSuccess = { accounts ->
                    displayBankAccounts(accounts)
                },
                onError = {
                    // Use local cache
                    val cachedAccounts = bankAccountRepository.getBankAccountsFromCache()
                    displayBankAccounts(cachedAccounts)
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error loading bank accounts", e)
            Toast.makeText(this, "Error loading bank accounts: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayBankAccounts(accounts: List<BankAccount>) {
        // Set up adapter
        bankAccountsAdapter = BankAccountsAdapter(
            accounts,
            onEdit = { account -> showAddBankAccountDialog(account) },
            onDelete = { account -> confirmDeleteBankAccount(account) }
        )
        
        recyclerViewBanking.adapter = bankAccountsAdapter
        
        // Mark data as loaded
        if (!dataLoaded) dataLoaded = true
        
        // Update empty view only if we're on the Banking tab
        if (tabLayout.selectedTabPosition == 2) {
            tvNoBanking.visibility = if (accounts.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun showAddBankAccountDialog(account: BankAccount?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bank_account, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etBankName = dialogView.findViewById<TextInputEditText>(R.id.etBankName)
        val etAccountNumber = dialogView.findViewById<TextInputEditText>(R.id.etAccountNumber)
        val etBranchCode = dialogView.findViewById<TextInputEditText>(R.id.etBranchCode)
        
        // Set title and populate fields if editing
        if (account != null) {
            tvDialogTitle.text = "Edit Bank Account"
            etBankName.setText(account.bankName)
            etAccountNumber.setText(account.accountNumber)
            etBranchCode.setText(account.branchCode)
        } else {
            tvDialogTitle.text = "Add Bank Account"
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val bankName = etBankName.text.toString().trim()
            val accountNumber = etAccountNumber.text.toString().trim()
            val branchCode = etBranchCode.text.toString().trim()
            
            // Validate inputs
            if (bankName.isEmpty() || accountNumber.isEmpty() || branchCode.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val newAccount = BankAccount(
                id = account?.id ?: UUID.randomUUID().toString(),
                bankName = bankName,
                accountNumber = accountNumber,
                branchCode = branchCode
            )
            
            if (account != null) {
                updateBankAccount(newAccount)
            } else {
                addBankAccount(newAccount)
            }
            
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun addBankAccount(account: BankAccount) {
        try {
            bankAccountRepository.saveBankAccountToFirestore(
                account,
                onSuccess = {
                    Toast.makeText(this, "Bank account added successfully", Toast.LENGTH_SHORT).show()
                    loadBankAccounts()
                },
                onError = { exception ->
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error adding bank account", e)
            Toast.makeText(this, "Error adding bank account: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateBankAccount(account: BankAccount) {
        try {
            bankAccountRepository.updateBankAccountInFirestore(
                account,
                onSuccess = {
                    Toast.makeText(this, "Bank account updated successfully", Toast.LENGTH_SHORT).show()
                    loadBankAccounts()
                },
                onError = { exception ->
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error updating bank account", e)
            Toast.makeText(this, "Error updating bank account: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun confirmDeleteBankAccount(account: BankAccount) {
        AlertDialog.Builder(this)
            .setTitle("Delete Bank Account")
            .setMessage("Are you sure you want to delete ${account.bankName}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteBankAccount(account)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteBankAccount(account: BankAccount) {
        try {
            bankAccountRepository.deleteBankAccountFromFirestore(
                account.id,
                onSuccess = {
                    Toast.makeText(this, "Bank account deleted successfully", Toast.LENGTH_SHORT).show()
                    loadBankAccounts()
                },
                onError = { exception ->
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error deleting bank account", e)
            Toast.makeText(this, "Error deleting bank account: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // ==================== Bookings Management Methods ====================
    
    private fun loadBookings() {
        try {
            // Fetch from Firestore, fall back to local cache
            bookingRepository.fetchBookingsFromFirestore(
                onSuccess = { bookings ->
                    displayBookings(bookings)
                },
                onError = {
                    // Use local cache
                    val cachedBookings = bookingRepository.getBookingsFromCache()
                    displayBookings(cachedBookings)
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error loading bookings", e)
            Toast.makeText(this, "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayBookings(bookings: List<Booking>) {
        // Sort bookings: pending first, then by date
        val sortedBookings = bookings.sortedWith(
            compareBy<Booking> { it.status != BookingStatus.PENDING }
                .thenByDescending { it.submittedDate }
        )
        
        // Set up adapter
        bookingsAdapter = AdminBookingsAdapter(
            sortedBookings,
            onApprove = { booking -> approveBooking(booking) },
            onDeny = { booking -> showDenyBookingDialog(booking) },
            onViewDetails = { booking -> showBookingDetailsDialog(booking) }
        )
        
        recyclerViewBookings.adapter = bookingsAdapter
        
        // Mark data as loaded
        if (!dataLoaded) dataLoaded = true
        
        // Update empty view only if we're on the Bookings tab
        if (tabLayout.selectedTabPosition == 3) {
            tvNoBookings.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun approveBooking(booking: Booking) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_approve_booking, null)
        val etMessage = dialogView.findViewById<TextInputEditText>(R.id.etMessage)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Approve Booking")
            .setMessage("Approve booking for '${booking.eventName}' by ${booking.userName}?")
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btnApprove).setOnClickListener {
            val message = etMessage.text.toString().trim()
            
            val updatedBooking = booking.copy(
                status = BookingStatus.APPROVED,
                adminMessage = message.ifEmpty { "Your booking has been approved!" },
                reviewedDate = Date(),
                reviewedBy = currentUser?.name ?: "Admin"
            )
            
            updateBookingStatus(updatedBooking)
            
            // Add approved booking as event to calendar
            addBookingAsEvent(updatedBooking)
            
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showDenyBookingDialog(booking: Booking) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_deny_booking, null)
        val etMessage = dialogView.findViewById<TextInputEditText>(R.id.etMessage)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Deny Booking")
            .setMessage("Deny booking for '${booking.eventName}' by ${booking.userName}?")
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btnDeny).setOnClickListener {
            val message = etMessage.text.toString().trim()
            
            if (message.isEmpty()) {
                Toast.makeText(this, "Please provide a reason for denial", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val updatedBooking = booking.copy(
                status = BookingStatus.DENIED,
                adminMessage = message,
                reviewedDate = Date(),
                reviewedBy = currentUser?.name ?: "Admin"
            )
            
            updateBookingStatus(updatedBooking)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateBookingStatus(booking: Booking) {
        try {
            bookingRepository.updateBookingInFirestore(
                booking,
                onSuccess = {
                    Toast.makeText(this, "Booking ${booking.getStatusString()}", Toast.LENGTH_SHORT).show()
                    loadBookings()
                },
                onError = { exception ->
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error updating booking", e)
            Toast.makeText(this, "Error updating booking: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun addBookingAsEvent(booking: Booking) {
        try {
            // Create an event from the approved booking
            val event = Event(
                id = UUID.randomUUID().toString(),
                title = "${booking.eventName} (Booked)",
                description = "Booked by: ${booking.userName}\n\n${booking.description}",
                date = booking.requestedDate,
                time = booking.requestedTime,
                location = "Umbilo Temple",
                requirements = "Contact: ${booking.contactNumber}",
                dresscode = "",
                categoryId = "temple-booking",
                isFirewalking = false
            )
            
            // Get existing events
            val events = localDatabase.getEvents().toMutableList()
            events.add(event)
            
            // Save to database
            localDatabase.saveEvents(events)
            
            Log.d("AdminActivity", "Added approved booking as event: ${event.title}")
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error adding booking as event", e)
        }
    }
    
    private fun showBookingDetailsDialog(booking: Booking) {
        val dateFormatFull = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        val message = """
            Event: ${booking.eventName}
            Requested by: ${booking.userName}
            Email: ${booking.userEmail}
            Contact: ${booking.contactNumber}
            
            Date: ${dateFormatFull.format(booking.requestedDate)}
            Time: ${booking.requestedTime}
            Estimated Attendees: ${booking.estimatedAttendees}
            
            Description:
            ${booking.description}
            
            Status: ${booking.getStatusString()}
            Submitted: ${dateFormatFull.format(booking.submittedDate)}
            ${if (booking.reviewedDate != null) "Reviewed: ${dateFormatFull.format(booking.reviewedDate)}" else ""}
            ${if (booking.reviewedBy != null) "Reviewed by: ${booking.reviewedBy}" else ""}
            ${if (booking.adminMessage.isNotEmpty()) "\nAdmin Message:\n${booking.adminMessage}" else ""}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun loadDocuments() {
        try {
            // Fetch documents from Firebase, fallback to cache
            documentRepository.fetchDocumentsFromFirestore(
                onSuccess = { documents ->
                    allDocuments = documents
                    // Only display if in USER_DOCUMENTS mode, otherwise let the user select
                    if (documentsViewMode == DocumentsViewMode.USER_DOCUMENTS && selectedUserEmail != null) {
                        displayDocuments(documents.filter { it.uploadedByEmail == selectedUserEmail })
                    }
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error fetching documents from Firebase", exception)
                    // Use cached documents
                    val cachedDocuments = documentRepository.getDocumentsFromCache()
                    allDocuments = cachedDocuments
                    if (documentsViewMode == DocumentsViewMode.USER_DOCUMENTS && selectedUserEmail != null) {
                        displayDocuments(cachedDocuments.filter { it.uploadedByEmail == selectedUserEmail })
                    }
                    Toast.makeText(this, "Using cached documents (offline mode)", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error loading documents", e)
            Toast.makeText(this, "Error loading documents: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadGallery() {
        try {
            // Fetch pending gallery media from Firebase
            documentRepository.getPendingGalleryMedia(
                onSuccess = { documents ->
                    displayGallery(documents)
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error fetching pending gallery media", exception)
                    Toast.makeText(this, "Error loading gallery media: ${exception.message}", Toast.LENGTH_SHORT).show()
                    displayGallery(emptyList())
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error loading gallery", e)
            Toast.makeText(this, "Error loading gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayGallery(documents: List<Document>) {
        // Set up adapter
        galleryAdapter = AdminGalleryAdapter(this, documents, object : AdminGalleryAdapter.GalleryActionListener {
            override fun onApproveMedia(document: Document, position: Int) {
                approveGalleryMedia(document, position)
            }
            
            override fun onRejectMedia(document: Document, position: Int) {
                rejectGalleryMedia(document, position)
            }
            
            override fun onViewMediaDetails(document: Document, position: Int) {
                showGalleryMediaDetails(document)
            }
        })
        
        recyclerViewGallery.adapter = galleryAdapter
        
        // Mark data as loaded
        if (!dataLoaded) dataLoaded = true
        
        // Update empty view only if we're on the Gallery tab
        if (tabLayout.selectedTabPosition == 4) {
            tvNoGallery.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun approveGalleryMedia(document: Document, position: Int) {
        documentRepository.approveOrRejectMedia(
            documentId = document.id,
            status = MediaStatus.APPROVED,
            adminMessage = "Approved by admin",
            reviewedBy = currentUser?.name,
            onSuccess = {
                Toast.makeText(this, "Media approved", Toast.LENGTH_SHORT).show()
                loadGallery() // Reload to refresh the list
            },
            onError = { exception ->
                Log.e("AdminActivity", "Error approving media", exception)
                Toast.makeText(this, "Error approving media: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun rejectGalleryMedia(document: Document, position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reject_media, null)
        val etRejectReason = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etRejectReason)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reject Media")
            .setView(dialogView)
            .setPositiveButton("Reject") { _, _ ->
                val reason = etRejectReason.text.toString().trim()
                
                documentRepository.approveOrRejectMedia(
                    documentId = document.id,
                    status = MediaStatus.REJECTED,
                    adminMessage = reason,
                    reviewedBy = currentUser?.name,
                    onSuccess = {
                        Toast.makeText(this, "Media rejected", Toast.LENGTH_SHORT).show()
                        loadGallery() // Reload to refresh the list
                    },
                    onError = { exception ->
                        Log.e("AdminActivity", "Error rejecting media", exception)
                        Toast.makeText(this, "Error rejecting media: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showGalleryMediaDetails(document: Document) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        
        var message = """
            Media: ${document.name}
            Description: ${document.description}
            
            Uploaded by: ${document.uploadedBy}
            Email: ${document.uploadedByEmail}
            Date: ${dateFormat.format(document.uploadDate)}
            
            File Type: ${document.fileType}
            Status: ${document.mediaStatus.name}
        """.trimIndent()
        
        if (document.reviewedBy != null) {
            message += "\n\nReviewed by: ${document.reviewedBy}"
            message += "\nReviewed: ${dateFormat.format(document.reviewedDate ?: Date())}"
        }
        
        if (document.adminMessage.isNotEmpty()) {
            message += "\n\nAdmin Message: ${document.adminMessage}"
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Media Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun displayDocuments(documents: List<Document>) {
        allDocuments = documents  // Store all documents for filtering
        
        // Set up adapter
        documentsAdapter = AdminDocumentsAdapter(this, documents, object : AdminDocumentsAdapter.DocumentActionListener {
            override fun onDeleteDocument(document: Document, position: Int) {
                confirmDeleteDocument(document, position)
            }
            
            override fun onViewDocument(document: Document, position: Int) {
                viewDocumentDetails(document)
            }
        })
        
        recyclerViewDocuments.adapter = documentsAdapter
        
        // Mark data as loaded
        if (!dataLoaded) dataLoaded = true
        
        // Update empty view (documents adapter is used for user documents view)
        tvNoDocuments.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun confirmDeleteDocument(document: Document, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete '${document.name}'? This will also remove the file from Firebase Storage.")
            .setPositiveButton("Delete") { _, _ ->
                deleteDocument(document)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteDocument(document: Document) {
        try {
            documentRepository.deleteDocumentFromFirebase(
                document.id,
                onSuccess = {
                    Toast.makeText(this, "Document deleted successfully", Toast.LENGTH_SHORT).show()
                    // Immediate UI feedback: remove locally
                    if (::recyclerViewDocuments.isInitialized && recyclerViewDocuments.adapter is AdminDocumentsAdapter) {
                        val adapter = recyclerViewDocuments.adapter as AdminDocumentsAdapter
                        val remaining = adapter.getDocuments().filter { it.id != document.id }
                        adapter.updateDocuments(remaining)
                        tvNoDocuments.visibility = if (remaining.isEmpty()) View.VISIBLE else View.GONE
                    }
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error deleting document from Firebase", exception)
                    Toast.makeText(this, "Error deleting document: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error deleting document", e)
            Toast.makeText(this, "Error deleting document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun viewDocumentDetails(document: Document) {
        val dateFormatFull = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        val message = """
            Name: ${document.name}
            Type: ${document.fileType}
            Uploaded by: ${document.uploadedBy}
            Upload Date: ${dateFormatFull.format(document.uploadDate)}
            ${if (document.eventId != null) "Event ID: ${document.eventId}" else ""}
            Public: ${if (document.isPublic) "Yes" else "No"}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Document Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun handleFABClick() {
        // Always open quick actions regardless of tab
        showQuickActionMenu()
    }
    
    private fun showQuickActionMenu() {
        val items = arrayOf(
            "Refresh Data",
            "View All Users",
            "View All Bookings"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Quick Actions")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        // Refresh data
                        Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show()
                        loadUsers()
                        loadEvents()
                        loadBankAccounts()
                        loadBookings()
                        loadDocuments()
                        loadGallery()
                    }
                    1 -> tabLayout.getTabAt(0)?.select()
                    2 -> tabLayout.getTabAt(3)?.select()
                }
            }
            .show()
    }
    
    // ==================== Search and Filter Methods ====================
    
    private fun filterUsers(query: String) {
        if (query.isEmpty()) {
            usersAdapter.updateUsers(allUsers)
        } else {
            val filtered = allUsers.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.email.contains(query, ignoreCase = true) ||
                it.phoneNumber.contains(query, ignoreCase = true)
            }
            usersAdapter.updateUsers(filtered)
        }
    }
    
    private fun filterDocumentsUsers(query: String) {
        // Filter users by name for Documents tab
        if (query.isEmpty()) {
            displayDocumentsUsers(allUsers.filter { it.id != currentUser?.id })
        } else {
            val filtered = allUsers.filter { 
                it.id != currentUser?.id &&
                (it.name.contains(query, ignoreCase = true) || 
                 it.email.contains(query, ignoreCase = true))
            }
            displayDocumentsUsers(filtered)
        }
    }
    
    private fun filterUserDocuments(query: String) {
        // Filter selected user's documents
        if (selectedUserEmail == null) return
        
        val userDocuments = allDocuments.filter { it.uploadedByEmail == selectedUserEmail }
        
        if (query.isEmpty()) {
            documentsAdapter.updateDocuments(userDocuments)
        } else {
            val filtered = userDocuments.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true)
            }
            documentsAdapter.updateDocuments(filtered)
        }
        
        tvNoDocuments.visibility = if (documentsAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }
    
    private fun displayDocumentsUsers(users: List<User>) {
        // Create a special adapter that shows users but allows clicking to view their documents
        val usersAdapterForDocuments = UsersAdapter(this, users, object : UsersAdapter.UserActionListener {
            override fun onToggleAdminStatus(user: User, position: Int) {
                // Not applicable in documents view
            }
            
            override fun onDeleteUser(user: User, position: Int) {
                // Not applicable in documents view
            }
            
            override fun onViewUserDocuments(user: User, position: Int) {
                viewUserDocumentsFromDocumentsTab(user)
            }
        }, isDocumentsMode = true)
        
        recyclerViewDocuments.adapter = usersAdapterForDocuments
        tvNoDocuments.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun viewUserDocuments(user: User) {
        selectedUserEmail = user.email
        
        // Try to use already loaded documents first
        val userDocuments = allDocuments.filter { it.uploadedByEmail == user.email }
        
        Log.d("AdminActivity", "Viewing documents for user: ${user.email}")
        Log.d("AdminActivity", "Total documents loaded: ${allDocuments.size}")
        Log.d("AdminActivity", "User documents found: ${userDocuments.size}")
        
        // If we have documents for this user, show them
        if (userDocuments.isNotEmpty()) {
            showUserDocumentsDialog(user.name, userDocuments)
            return
        }
        
        // If we don't have documents yet, try loading from cache or Firebase
        if (allDocuments.isEmpty()) {
            Toast.makeText(this, "Loading documents...", Toast.LENGTH_SHORT).show()
            
            // Try cache first
            val cachedDocuments = documentRepository.getDocumentsFromCache()
            if (cachedDocuments.isNotEmpty()) {
                allDocuments = cachedDocuments
                val userDocsFromCache = allDocuments.filter { it.uploadedByEmail == user.email }
                if (userDocsFromCache.isNotEmpty()) {
                    showUserDocumentsDialog(user.name, userDocsFromCache)
                    return
                }
            }
            
            // Try loading from Firebase
            documentRepository.fetchDocumentsFromFirestore(
                onSuccess = { documents ->
                    allDocuments = documents
                    val userDocs = allDocuments.filter { it.uploadedByEmail == user.email }
                    
                    if (userDocs.isEmpty()) {
                        Toast.makeText(this, "No documents found for ${user.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        showUserDocumentsDialog(user.name, userDocs)
                    }
                },
                onError = { exception ->
                    Log.e("AdminActivity", "Error loading documents", exception)
                    Toast.makeText(this, "Error loading documents: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // Documents loaded but none for this user
            Toast.makeText(this, "No documents found for ${user.name}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showUserDocumentsDialog(userName: String, documents: List<Document>) {
        // Create a dialog to show all user's documents
        val documentListView = android.view.LayoutInflater.from(this)
            .inflate(R.layout.dialog_user_documents, null)
        
        val recyclerView = documentListView.findViewById<RecyclerView>(R.id.rvDialogDocuments)
        val tvHeader = documentListView.findViewById<TextView>(R.id.tvDialogTitle)
        
        tvHeader.text = "${userName}'s Documents"
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AdminDocumentsAdapter(this, documents, object : AdminDocumentsAdapter.DocumentActionListener {
            override fun onDeleteDocument(document: Document, position: Int) {
                confirmDeleteDocument(document, position)
            }
            
            override fun onViewDocument(document: Document, position: Int) {
                viewDocumentDetails(document)
            }
        })
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Documents for $userName")
            .setView(documentListView)
            .setPositiveButton("Close", null)
            .create()
        
        dialog.show()
    }
    
    private fun viewUserDocumentsFromDocumentsTab(user: User) {
        selectedUserEmail = user.email
        documentsViewMode = DocumentsViewMode.USER_DOCUMENTS
        
        // Switch to showing user's documents
        val userDocuments = allDocuments.filter { it.uploadedByEmail == user.email }
        
        // Update search hint BEFORE clearing the search field to prevent triggering reset
        etSearchDocuments.hint = "Search ${user.name}'s documents (clear to go back)..."
        
        // Set flag to prevent auto-reset
        isProgrammaticSearchChange = true
        // Clear search to reset it
        etSearchDocuments.setText("")
        
        // Display the user's documents
        displayDocuments(userDocuments)
        
        Toast.makeText(this, "Showing documents for ${user.name}", Toast.LENGTH_SHORT).show()
    }
    
    private fun showAllUsersForDocumentsTab() {
        selectedUserEmail = null
        documentsViewMode = DocumentsViewMode.USER_LIST
        
        // Show all users in documents tab
        filterDocumentsUsers("")
        
        // Reset search hint
        etSearchDocuments.hint = "Search users..."
        
        // Set flag to prevent auto-reset
        isProgrammaticSearchChange = true
        // Clear the search field
        etSearchDocuments.setText("")
    }
}
