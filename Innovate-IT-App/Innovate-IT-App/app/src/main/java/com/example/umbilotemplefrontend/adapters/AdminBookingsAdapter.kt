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
 * Adapter for displaying bookings in admin panel
 */
class AdminBookingsAdapter(
    private var bookings: List<Booking>,
    private val onApprove: (Booking) -> Unit,
    private val onDeny: (Booking) -> Unit,
    private val onViewDetails: (Booking) -> Unit
) : RecyclerView.Adapter<AdminBookingsAdapter.BookingViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvAttendees: TextView = itemView.findViewById(R.id.tvAttendees)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        val btnDeny: Button = itemView.findViewById(R.id.btnDeny)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        
        holder.tvEventName.text = booking.eventName
        holder.tvUserName.text = "By: ${booking.userName}"
        holder.tvDate.text = "Date: ${dateFormat.format(booking.requestedDate)} at ${booking.requestedTime}"
        holder.tvAttendees.text = "${booking.estimatedAttendees} attendees"
        holder.tvStatus.text = booking.getStatusString()
        
        // Set status color
        val statusColor = holder.itemView.resources.getColor(booking.getStatusColor(), null)
        holder.tvStatus.setTextColor(statusColor)
        
        // Show/hide action buttons based on status
        when (booking.status) {
            BookingStatus.PENDING -> {
                holder.btnApprove.visibility = View.VISIBLE
                holder.btnDeny.visibility = View.VISIBLE
            }
            else -> {
                holder.btnApprove.visibility = View.GONE
                holder.btnDeny.visibility = View.GONE
            }
        }
        
        // Set button listeners
        holder.btnApprove.setOnClickListener {
            onApprove(booking)
        }
        
        holder.btnDeny.setOnClickListener {
            onDeny(booking)
        }
        
        holder.btnViewDetails.setOnClickListener {
            onViewDetails(booking)
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

