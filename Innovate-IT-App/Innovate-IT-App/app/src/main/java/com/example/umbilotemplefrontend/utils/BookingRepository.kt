package com.example.umbilotemplefrontend.utils

import android.content.Context
import android.util.Log
import com.example.umbilotemplefrontend.models.Booking
import com.example.umbilotemplefrontend.models.BookingStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

/**
 * Repository for managing bookings with Firebase Firestore sync
 * Handles both cloud storage and local caching for offline access
 */
class BookingRepository(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val localDatabase = LocalDatabase(context)
    private val collectionName = "bookings"
    private var listenerRegistration: ListenerRegistration? = null
    
    companion object {
        private const val TAG = "BookingRepository"
    }
    
    /**
     * Fetch bookings from Firestore and cache locally
     */
    fun fetchBookingsFromFirestore(
        onSuccess: (List<Booking>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection(collectionName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val bookings = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        Booking(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            eventName = doc.getString("eventName") ?: "",
                            description = doc.getString("description") ?: "",
                            requestedDate = doc.getDate("requestedDate") ?: Date(),
                            requestedTime = doc.getString("requestedTime") ?: "",
                            estimatedAttendees = doc.getLong("estimatedAttendees")?.toInt() ?: 0,
                            contactNumber = doc.getString("contactNumber") ?: "",
                            status = BookingStatus.valueOf(doc.getString("status") ?: "PENDING"),
                            adminMessage = doc.getString("adminMessage") ?: "",
                            submittedDate = doc.getDate("submittedDate") ?: Date(),
                            reviewedDate = doc.getDate("reviewedDate"),
                            reviewedBy = doc.getString("reviewedBy")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing booking: ${doc.id}", e)
                        null
                    }
                }
                
                // Cache locally
                localDatabase.saveBookings(bookings)
                onSuccess(bookings)
                
                Log.d(TAG, "Fetched ${bookings.size} bookings from Firestore")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching bookings from Firestore", exception)
                // Return cached data on failure
                val cachedBookings = localDatabase.getBookings()
                if (cachedBookings.isNotEmpty()) {
                    Log.d(TAG, "Using ${cachedBookings.size} cached bookings")
                    onSuccess(cachedBookings)
                } else {
                    onError(exception)
                }
            }
    }
    
    /**
     * Save a booking to Firestore and local cache
     */
    fun saveBookingToFirestore(
        booking: Booking,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val bookingMap = hashMapOf(
            "userId" to booking.userId,
            "userName" to booking.userName,
            "userEmail" to booking.userEmail,
            "eventName" to booking.eventName,
            "description" to booking.description,
            "requestedDate" to booking.requestedDate,
            "requestedTime" to booking.requestedTime,
            "estimatedAttendees" to booking.estimatedAttendees,
            "contactNumber" to booking.contactNumber,
            "status" to booking.status.name,
            "adminMessage" to booking.adminMessage,
            "submittedDate" to booking.submittedDate,
            "reviewedDate" to booking.reviewedDate,
            "reviewedBy" to booking.reviewedBy
        )
        
        firestore.collection(collectionName)
            .document(booking.id)
            .set(bookingMap)
            .addOnSuccessListener {
                // Save to local cache
                localDatabase.addBooking(booking)
                onSuccess()
                Log.d(TAG, "Booking saved to Firestore: ${booking.eventName}")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error saving booking to Firestore", exception)
                // Still save locally for offline use
                localDatabase.addBooking(booking)
                onError(exception)
            }
    }
    
    /**
     * Update a booking in Firestore and local cache
     */
    fun updateBookingInFirestore(
        booking: Booking,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val bookingMap = hashMapOf(
            "userId" to booking.userId,
            "userName" to booking.userName,
            "userEmail" to booking.userEmail,
            "eventName" to booking.eventName,
            "description" to booking.description,
            "requestedDate" to booking.requestedDate,
            "requestedTime" to booking.requestedTime,
            "estimatedAttendees" to booking.estimatedAttendees,
            "contactNumber" to booking.contactNumber,
            "status" to booking.status.name,
            "adminMessage" to booking.adminMessage,
            "submittedDate" to booking.submittedDate,
            "reviewedDate" to booking.reviewedDate,
            "reviewedBy" to booking.reviewedBy
        )
        
        firestore.collection(collectionName)
            .document(booking.id)
            .update(bookingMap as Map<String, Any>)
            .addOnSuccessListener {
                // Update local cache
                localDatabase.updateBooking(booking)
                onSuccess()
                Log.d(TAG, "Booking updated in Firestore: ${booking.eventName}")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error updating booking in Firestore", exception)
                // Still update locally
                localDatabase.updateBooking(booking)
                onError(exception)
            }
    }
    
    /**
     * Delete a booking from Firestore and local cache
     */
    fun deleteBookingFromFirestore(
        bookingId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection(collectionName)
            .document(bookingId)
            .delete()
            .addOnSuccessListener {
                // Delete from local cache
                localDatabase.deleteBooking(bookingId)
                onSuccess()
                Log.d(TAG, "Booking deleted from Firestore: $bookingId")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting booking from Firestore", exception)
                // Still delete locally
                localDatabase.deleteBooking(bookingId)
                onError(exception)
            }
    }
    
    /**
     * Set up real-time listener for bookings
     * Automatically updates local cache when cloud data changes
     */
    fun startRealtimeListener(onUpdate: (List<Booking>) -> Unit) {
        listenerRegistration = firestore.collection(collectionName)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to bookings", error)
                    return@addSnapshotListener
                }
                
                if (querySnapshot != null) {
                    val bookings = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            Booking(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                userName = doc.getString("userName") ?: "",
                                userEmail = doc.getString("userEmail") ?: "",
                                eventName = doc.getString("eventName") ?: "",
                                description = doc.getString("description") ?: "",
                                requestedDate = doc.getDate("requestedDate") ?: Date(),
                                requestedTime = doc.getString("requestedTime") ?: "",
                                estimatedAttendees = doc.getLong("estimatedAttendees")?.toInt() ?: 0,
                                contactNumber = doc.getString("contactNumber") ?: "",
                                status = BookingStatus.valueOf(doc.getString("status") ?: "PENDING"),
                                adminMessage = doc.getString("adminMessage") ?: "",
                                submittedDate = doc.getDate("submittedDate") ?: Date(),
                                reviewedDate = doc.getDate("reviewedDate"),
                                reviewedBy = doc.getString("reviewedBy")
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing booking: ${doc.id}", e)
                            null
                        }
                    }
                    
                    // Update local cache
                    localDatabase.saveBookings(bookings)
                    onUpdate(bookings)
                    
                    Log.d(TAG, "Real-time update: ${bookings.size} bookings")
                }
            }
    }
    
    /**
     * Stop the real-time listener
     */
    fun stopRealtimeListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d(TAG, "Real-time listener stopped")
    }
    
    /**
     * Get bookings from local cache (for offline access)
     */
    fun getBookingsFromCache(): List<Booking> {
        return localDatabase.getBookings()
    }
    
    /**
     * Get bookings for a specific user from cache
     */
    fun getUserBookingsFromCache(userId: String): List<Booking> {
        return localDatabase.getBookingsByUserId(userId)
    }
}

