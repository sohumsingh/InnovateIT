package com.example.umbilotemplefrontend.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.Event
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AgendaAdapter(
    private val context: Context,
    private var events: List<Event>,
    private val onEventClickListener: (Event) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_EVENT = 1
    }

    private val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val agendaItems = mutableListOf<AgendaItem>()

    init {
        processEvents(events)
    }

    fun updateEvents(newEvents: List<Event>) {
        this.events = newEvents
        processEvents(newEvents)
        notifyDataSetChanged()
    }

    private fun processEvents(events: List<Event>) {
        // Group events by date
        val eventsByDate = events.sortedBy { it.date }.groupBy { getDateWithoutTime(it.date) }
        
        agendaItems.clear()
        
        // Create agenda items
        for ((date, dateEvents) in eventsByDate) {
            // Add date header
            agendaItems.add(AgendaItem.DateHeader(date, dateEvents.size))
            
            // Add events for this date
            for (event in dateEvents) {
                agendaItems.add(AgendaItem.EventItem(event))
            }
        }
    }

    private fun getDateWithoutTime(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    override fun getItemViewType(position: Int): Int {
        return when (agendaItems[position]) {
            is AgendaItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is AgendaItem.EventItem -> VIEW_TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_agenda_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            VIEW_TYPE_EVENT -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_agenda_event, parent, false)
                EventViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = agendaItems[position]) {
            is AgendaItem.DateHeader -> {
                val dateHolder = holder as DateHeaderViewHolder
                dateHolder.tvDate.text = dateFormat.format(item.date)
                dateHolder.tvEventCount.text = "(${item.eventCount} event${if (item.eventCount > 1) "s" else ""})"
            }
            is AgendaItem.EventItem -> {
                val eventHolder = holder as EventViewHolder
                val event = item.event
                
                // Set event details
                eventHolder.tvTitle.text = event.title
                eventHolder.tvTime.text = if (event.allDay) {
                    "All day"
                } else if (event.endTime != null) {
                    "${event.time} - ${event.endTime}"
                } else {
                    event.time
                }
                eventHolder.tvLocation.text = event.location
                
                // Set category color
                val category = event.getCategory()
                eventHolder.viewCategoryIndicator.setBackgroundColor(category.color)
                
                // Set click listener
                holder.itemView.setOnClickListener {
                    onEventClickListener(event)
                }
            }
        }
    }

    override fun getItemCount(): Int = agendaItems.size

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvAgendaDate)
        val tvEventCount: TextView = view.findViewById(R.id.tvAgendaEventCount)
    }

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewCategoryIndicator: View = view.findViewById(R.id.viewCategoryIndicator)
        val tvTime: TextView = view.findViewById(R.id.tvAgendaEventTime)
        val tvTitle: TextView = view.findViewById(R.id.tvAgendaEventTitle)
        val tvLocation: TextView = view.findViewById(R.id.tvAgendaEventLocation)
    }

    sealed class AgendaItem {
        data class DateHeader(val date: Date, val eventCount: Int) : AgendaItem()
        data class EventItem(val event: Event) : AgendaItem()
    }
}
