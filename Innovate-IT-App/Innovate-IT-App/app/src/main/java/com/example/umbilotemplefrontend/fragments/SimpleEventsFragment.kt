package com.example.umbilotemplefrontend.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.Event
import com.example.umbilotemplefrontend.models.User
import com.example.umbilotemplefrontend.utils.LocalDatabase
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A simplified version of the EventsAdminFragment for testing
 */
class SimpleEventsFragment : Fragment() {

    private lateinit var localDatabase: LocalDatabase
    private val events = mutableListOf<Event>()
    private var currentUser: User? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("SimpleEventsFragment", "onCreateView started")
        
        try {
            // Create a simple TextView instead of using a layout file
            val textView = TextView(requireContext())
            textView.setPadding(32, 32, 32, 32)
            textView.textSize = 16f
            
            // Initialize database
            localDatabase = LocalDatabase(requireContext())
            
            // Get current user
            currentUser = localDatabase.getCurrentUser()
            Log.d("SimpleEventsFragment", "Current user: ${currentUser?.name}, isAdmin: ${currentUser?.isAdmin}")
            
            // Load events
            loadEvents()
            
            // Display event list
            val eventList = StringBuilder()
            eventList.append("Events (${events.size}):\n\n")
            
            events.forEachIndexed { index, event ->
                eventList.append("${index + 1}. ${event.title}\n")
                eventList.append("   Date: ${dateFormat.format(event.date)}\n")
                eventList.append("   Time: ${event.time}\n")
                eventList.append("   Location: ${event.location}\n\n")
            }
            
            textView.text = eventList.toString()
            
            Log.d("SimpleEventsFragment", "onCreateView completed")
            return textView
            
        } catch (e: Exception) {
            Log.e("SimpleEventsFragment", "Error in onCreateView", e)
            val errorView = TextView(requireContext())
            errorView.text = "Error loading events: ${e.message}"
            return errorView
        }
    }
    
    private fun loadEvents() {
        Log.d("SimpleEventsFragment", "Loading events")
        try {
            // Get all events
            val allEvents = localDatabase.getEvents()
            Log.d("SimpleEventsFragment", "Found ${allEvents.size} events")
            
            events.clear()
            events.addAll(allEvents)
        } catch (e: Exception) {
            Log.e("SimpleEventsFragment", "Error loading events", e)
        }
    }
}
