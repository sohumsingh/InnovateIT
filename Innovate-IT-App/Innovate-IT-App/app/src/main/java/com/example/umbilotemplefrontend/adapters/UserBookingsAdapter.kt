package com.example.umbilotemplefrontend.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.Booking
import com.example.umbilotemplefrontend.models.BookingStatus
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying user's bookings
 */
class UserBookingsAdapter(
    private var bookings: List<Booking>,
    private val onViewDetails: (Booking) -> Unit,
    private val onViewOnCalendar: (Booking) -> Unit
) : RecyclerView.Adapter<UserBookingsAdapter.BookingViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvAdminMessage: TextView = itemView.findViewById(R.id.tvAdminMessage)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
        val btnViewOnCalendar: Button = itemView.findViewById(R.id.btnViewOnCalendar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        
        holder.tvEventName.text = booking.eventName
        holder.tvDate.text = "Date: ${dateFormat.format(booking.requestedDate)} at ${booking.requestedTime}"
        holder.tvStatus.text = booking.getStatusString()
        
        // Set status color
        val statusColor = holder.itemView.resources.getColor(booking.getStatusColor(), null)
        holder.tvStatus.setTextColor(statusColor)
        
        // Show/hide admin message
        if (booking.adminMessage.isNotEmpty()) {
            holder.tvAdminMessage.visibility = View.VISIBLE
            holder.tvAdminMessage.text = "Message: ${booking.adminMessage}"
        } else {
            holder.tvAdminMessage.visibility = View.GONE
        }
        
        // Show/hide "View on Calendar" button
        if (booking.status == BookingStatus.APPROVED) {
            holder.btnViewOnCalendar.visibility = View.VISIBLE
        } else {
            holder.btnViewOnCalendar.visibility = View.GONE
        }
        
        // Set button listeners
        holder.btnViewDetails.setOnClickListener {
            onViewDetails(booking)
        }
        
        holder.btnViewOnCalendar.setOnClickListener {
            onViewOnCalendar(booking)
        }
    }

    override fun getItemCount(): Int = bookings.size

    /**
     * Update the list of bookings and refresh the view
     */
    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}

