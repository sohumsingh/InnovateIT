package com.example.umbilotemplefrontend.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.adapters.EventsAdapter
import com.example.umbilotemplefrontend.models.Event
import com.example.umbilotemplefrontend.models.User
import com.example.umbilotemplefrontend.utils.LocalDatabase

class EventsAdminFragment : Fragment(), EventsAdapter.EventActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var localDatabase: LocalDatabase
    private val events = mutableListOf<Event>()
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_events_admin, container, false)
        
        // Initialize database
        localDatabase = LocalDatabase(requireContext())
        
        // Get current user
        currentUser = localDatabase.getCurrentUser()
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.eventsAdminRecyclerView)
        eventsAdapter = EventsAdapter(requireContext(), events, this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = eventsAdapter
        
        // Load events
        loadEvents()
        
        return view
    }
    
    private fun loadEvents() {
        // Get all events
        val allEvents = localDatabase.getEvents()
        
        events.clear()
        events.addAll(allEvents)
        eventsAdapter.notifyDataSetChanged()
    }
    
    override fun onDeleteEvent(position: Int) {
        // Check if current user is admin
        if (currentUser?.isAdmin != true) {
            Toast.makeText(requireContext(), "You don't have admin privileges", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Remove event
        events.removeAt(position)
        
        // Save to database
        localDatabase.saveEvents(events)
        
        // Update UI
        eventsAdapter.notifyItemRemoved(position)
        Toast.makeText(requireContext(), "Event deleted", Toast.LENGTH_SHORT).show()
    }
    
    fun refreshEvents() {
        loadEvents()
    }
}
