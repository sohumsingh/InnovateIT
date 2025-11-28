package com.example.umbilotemplefrontend

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.adapters.UserBookingsAdapter
import com.example.umbilotemplefrontend.models.Booking
import com.example.umbilotemplefrontend.utils.BookingRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.NavigationHandler

/**
 * Activity for users to view and manage their booking requests
 */
class BookingsActivity : AppCompatActivity() {
    
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var localDatabase: LocalDatabase
    private lateinit var bookingRepository: BookingRepository
    private lateinit var recyclerViewBookings: RecyclerView
    private lateinit var tvNoBookings: TextView
    private lateinit var btnRequestBooking: Button
    private lateinit var bookingsAdapter: UserBookingsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookings)
        
        // Initialize database and repository
        localDatabase = LocalDatabase(this)
        bookingRepository = BookingRepository(this)
        
        // Initialize navigation handler
        navigationHandler = NavigationHandler(this)
        
        // Check if user is logged in
        if (!navigationHandler.checkUserLoggedIn()) {
            return
        }
        
        // Set up navigation
        navigationHandler.setupNavigation()
        
        // Initialize views
        recyclerViewBookings = findViewById(R.id.recyclerViewBookings)
        tvNoBookings = findViewById(R.id.tvNoBookings)
        btnRequestBooking = findViewById(R.id.btnRequestBooking)
        // Header title
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "MY BOOKINGS"
        
        // Set up back button
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            // Navigate to home page instead of closing app
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        // Set up RecyclerView
        recyclerViewBookings.layoutManager = LinearLayoutManager(this)
        
        // Set up request booking button
        btnRequestBooking.setOnClickListener {
            // Go to calendar to select date
            val intent = Intent(this, CalendarActivity::class.java)
            intent.putExtra("REQUEST_BOOKING", true)
            startActivity(intent)
        }
        
        // Load user's bookings
        loadBookings()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh bookings when returning to this screen
        loadBookings()
    }
    
    private fun loadBookings() {
        val currentUser = localDatabase.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Try to fetch from Firestore first, fall back to local cache
        bookingRepository.fetchBookingsFromFirestore(
            onSuccess = { allBookings ->
                // Filter bookings for current user
                val userBookings = allBookings.filter { it.userId == currentUser.id }
                    .sortedByDescending { it.submittedDate }
                displayBookings(userBookings)
            },
            onError = { exception ->
                // Use local cache
                val cachedBookings = bookingRepository.getUserBookingsFromCache(currentUser.id)
                    .sortedByDescending { it.submittedDate }
                displayBookings(cachedBookings)
                Toast.makeText(
                    this,
                    "Using cached bookings (offline mode)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
    
    private fun displayBookings(bookings: List<Booking>) {
        if (bookings.isEmpty()) {
            tvNoBookings.visibility = View.VISIBLE
            recyclerViewBookings.visibility = View.GONE
            return
        }
        
        tvNoBookings.visibility = View.GONE
        recyclerViewBookings.visibility = View.VISIBLE
        
        bookingsAdapter = UserBookingsAdapter(
            bookings,
            onViewDetails = { booking -> showBookingDetails(booking) },
            onViewOnCalendar = { booking -> viewBookingOnCalendar(booking) }
        )
        
        recyclerViewBookings.adapter = bookingsAdapter
    }
    
    private fun showBookingDetails(booking: Booking) {
        val statusColor = resources.getColor(booking.getStatusColor(), null)
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        
        var message = """
            Event: ${booking.eventName}
            Date: ${dateFormat.format(booking.requestedDate)}
            Time: ${booking.requestedTime}
            Attendees: ${booking.estimatedAttendees}
            Contact: ${booking.contactNumber}
            Status: ${booking.getStatusString()}
            
            Description:
            ${booking.description}
        """.trimIndent()
        
        if (booking.adminMessage.isNotEmpty()) {
            message += "\n\nAdmin Message:\n${booking.adminMessage}"
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }
    
    private fun viewBookingOnCalendar(booking: Booking) {
        if (booking.status != com.example.umbilotemplefrontend.models.BookingStatus.APPROVED) {
            Toast.makeText(
                this,
                "Only approved bookings appear on the calendar",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        val intent = Intent(this, CalendarActivity::class.java)
        intent.putExtra("SELECTED_DATE", booking.requestedDate.time)
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop real-time listener if it was started
        bookingRepository.stopRealtimeListener()
    }
}

