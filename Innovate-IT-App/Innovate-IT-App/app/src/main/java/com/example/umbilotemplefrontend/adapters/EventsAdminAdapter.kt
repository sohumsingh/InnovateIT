package com.example.umbilotemplefrontend.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.Event
import java.text.SimpleDateFormat
import java.util.Locale

class EventsAdminAdapter(
    private val context: Context,
    private var events: List<Event>,
    private val listener: EventActionListener
) : RecyclerView.Adapter<EventsAdminAdapter.EventViewHolder>() {

    interface EventActionListener {
        fun onDeleteEvent(event: Event, position: Int)
        fun onViewEventDetails(event: Event, position: Int)
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEventTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvEventDate: TextView = view.findViewById(R.id.tvEventDate)
        val tvEventTime: TextView = view.findViewById(R.id.tvEventTime)
        val tvEventLocation: TextView = view.findViewById(R.id.tvEventLocation)
        val btnViewDetails: TextView = view.findViewById(R.id.btnViewDetails)
        val btnDeleteEvent: ImageButton = view.findViewById(R.id.btnDeleteEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_admin, parent, false)
        return EventViewHolder(view)
    }

    override fun getItemCount() = events.size

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        
        holder.tvEventTitle.text = event.title
        holder.tvEventDate.text = dateFormat.format(event.date)
        holder.tvEventTime.text = event.time
        holder.tvEventLocation.text = event.location
        
        // Set button click listeners
        holder.btnViewDetails.setOnClickListener {
            listener.onViewEventDetails(event, position)
        }
        
        holder.btnDeleteEvent.setOnClickListener {
            listener.onDeleteEvent(event, position)
        }
    }

    fun updateEvents(newEvents: List<Event>) {
        this.events = newEvents
        notifyDataSetChanged()
    }
    
    fun getEvents(): List<Event> {
        return events
    }
}
