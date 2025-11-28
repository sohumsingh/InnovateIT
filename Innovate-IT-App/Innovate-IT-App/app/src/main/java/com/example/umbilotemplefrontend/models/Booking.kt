package com.example.umbilotemplefrontend.models

import java.io.Serializable
import java.util.Date

/**
 * Booking status enum
 */
enum class BookingStatus {
    PENDING,
    APPROVED,
    DENIED
}

/**
 * Model class for Temple Booking Requests
 */
data class Booking(
    val id: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val eventName: String,
    val description: String,
    val requestedDate: Date,
    val requestedTime: String,
    val estimatedAttendees: Int,
    val contactNumber: String,
    val status: BookingStatus = BookingStatus.PENDING,
    val adminMessage: String = "", // Message from admin when denied/approved
    val submittedDate: Date = Date(),
    val reviewedDate: Date? = null, // Date when admin approved/denied
    val reviewedBy: String? = null // Admin who reviewed
) : Serializable {
    
    /**
     * Get status as display string
     */
    fun getStatusString(): String {
        return when (status) {
            BookingStatus.PENDING -> "Pending Review"
            BookingStatus.APPROVED -> "Approved"
            BookingStatus.DENIED -> "Denied"
        }
    }
    
    /**
     * Get status color resource
     */
    fun getStatusColor(): Int {
        return when (status) {
            BookingStatus.PENDING -> android.R.color.holo_orange_dark
            BookingStatus.APPROVED -> android.R.color.holo_green_dark
            BookingStatus.DENIED -> android.R.color.holo_red_dark
        }
    }
    
    /**
     * Check if booking is for a past date
     */
    fun isPastDate(): Boolean {
        val today = Date()
        return requestedDate.before(today)
    }
}

