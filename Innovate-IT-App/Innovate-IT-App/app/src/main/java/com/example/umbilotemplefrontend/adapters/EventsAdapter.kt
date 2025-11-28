package com.example.umbilotemplefrontend.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.FirewalkingDetailsActivity
import com.example.umbilotemplefrontend.R
import androidx.core.content.ContextCompat
import com.example.umbilotemplefrontend.models.Event
import java.text.SimpleDateFormat
import java.util.Locale

class EventsAdapter(
    private val context: Context,
    private var events: List<Event>,
    private val actionListener: EventActionListener? = null
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    interface EventActionListener {
        fun onDeleteEvent(position: Int)
    }

    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvDate: TextView = view.findViewById(R.id.tvEventDate)
        val tvTime: TextView = view.findViewById(R.id.tvEventTime)
        val tvDescription: TextView = view.findViewById(R.id.tvEventDescription)
        val tvLocation: TextView = view.findViewById(R.id.tvEventLocation)
        val tvRequirements: TextView = view.findViewById(R.id.tvEventRequirements)
        val tvDressCode: TextView = view.findViewById(R.id.tvEventDressCode)
        val btnToggleDetails: Button = view.findViewById(R.id.btnToggleDetails)
        val btnDeleteEvent: ImageButton = view.findViewById(R.id.btnDeleteEvent)
        val layoutEventDetails: LinearLayout = view.findViewById(R.id.layoutEventDetails)
        val layoutMedia: LinearLayout = view.findViewById(R.id.layoutMedia)
        val layoutMediaItems: LinearLayout = view.findViewById(R.id.layoutMediaItems)
        val layoutDocuments: LinearLayout = view.findViewById(R.id.layoutDocuments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun getItemCount() = events.size

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        
        holder.tvTitle.text = event.title
        holder.tvDate.text = dateFormat.format(event.date)
        holder.tvTime.text = event.time
        holder.tvDescription.text = event.description
        holder.tvLocation.text = event.location
        holder.tvRequirements.text = event.requirements
        holder.tvDressCode.text = event.dresscode
        
        // Set category color indicator if available
        try {
            val category = event.getCategory()
            holder.tvTitle.setTextColor(category.color)
        } catch (e: Exception) {
            // If category access fails, use theme-aware default color
            holder.tvTitle.setTextColor(
                ContextCompat.getColor(context, R.color.text_on_bg)
            )
        }

        // Toggle event details visibility
        holder.btnToggleDetails.setOnClickListener {
            val isVisible = holder.layoutEventDetails.visibility == View.VISIBLE
            holder.layoutEventDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.btnToggleDetails.text = if (isVisible) "Show Details" else "Hide Details"
        }

        // Set delete button visibility and click listener
        if (actionListener != null) {
            holder.btnDeleteEvent.visibility = View.VISIBLE
            holder.btnDeleteEvent.setOnClickListener {
                actionListener.onDeleteEvent(position)
            }
        } else {
            holder.btnDeleteEvent.visibility = View.GONE
        }

        // For firewalking events, add a special action
        if (event.isFirewalking) {
            holder.itemView.setOnClickListener {
                val intent = Intent(context, FirewalkingDetailsActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                context.startActivity(intent)
            }
        }

        // Handle media items if available
        if (event.images.isNotEmpty() || event.videos.isNotEmpty()) {
            holder.layoutMedia.visibility = View.VISIBLE
            holder.layoutMediaItems.removeAllViews()
            
            // Add thumbnails for images and videos
            // This is a simplified implementation
            for (imagePath in event.images) {
                val imageView = LayoutInflater.from(context).inflate(
                    R.layout.item_media_thumbnail, holder.layoutMediaItems, false
                )
                holder.layoutMediaItems.addView(imageView)
            }
            
            for (videoPath in event.videos) {
                val videoView = LayoutInflater.from(context).inflate(
                    R.layout.item_media_thumbnail, holder.layoutMediaItems, false
                )
                holder.layoutMediaItems.addView(videoView)
            }
        } else {
            holder.layoutMedia.visibility = View.GONE
        }

        // Handle documents if available
        if (event.documents.isNotEmpty()) {
            holder.layoutDocuments.visibility = View.VISIBLE
            // Documents would be handled by another adapter
        } else {
            holder.layoutDocuments.visibility = View.GONE
        }
    }

    fun updateEvents(newEvents: List<Event>) {
        this.events = newEvents
        notifyDataSetChanged()
    }
} 