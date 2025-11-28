package com.example.umbilotemplefrontend.utils

import android.content.Context
import android.util.Log
import com.example.umbilotemplefrontend.models.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

/**
 * Repository for managing Event data with Firebase Firestore sync
 */
class EventRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val eventsCollection = db.collection("events")
    private val localDatabase = LocalDatabase(context)
    private var eventsListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "EventRepository"
    }

    /**
     * Fetch all events from Firestore
     */
    fun fetchEventsFromFirestore(
        onSuccess: (List<Event>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        eventsCollection.get()
            .addOnSuccessListener { snapshot ->
                val events = snapshot.documents.mapNotNull { doc ->
                    try {
                        documentToEvent(doc.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing event: ${doc.id}", e)
                        null
                    }
                }
                // Cache locally
                saveEventsToCache(events)
                onSuccess(events)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching events from Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Save a single event to Firestore
     */
    fun saveEventToFirestore(
        event: Event,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val eventMap = eventToMap(event)
        eventsCollection.document(event.id).set(eventMap)
            .addOnSuccessListener {
                Log.d(TAG, "Event saved to Firestore: ${event.id}")
                // Update local cache
                val events = localDatabase.getEvents().toMutableList()
                val existingIndex = events.indexOfFirst { it.id == event.id }
                if (existingIndex >= 0) {
                    events[existingIndex] = event
                } else {
                    events.add(event)
                }
                localDatabase.saveEvents(events)
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error saving event to Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Update an existing event in Firestore
     */
    fun updateEventInFirestore(
        event: Event,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        saveEventToFirestore(event, onSuccess, onError)
    }

    /**
     * Delete an event from Firestore
     */
    fun deleteEventFromFirestore(
        eventId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        eventsCollection.document(eventId).delete()
            .addOnSuccessListener {
                Log.d(TAG, "Event deleted from Firestore: $eventId")
                // Update local cache
                val events = localDatabase.getEvents().toMutableList()
                val iterator = events.iterator()
                while (iterator.hasNext()) {
                    if (iterator.next().id == eventId) {
                        iterator.remove()
                        break
                    }
                }
                localDatabase.saveEvents(events)
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting event from Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Get events from local cache
     */
    fun getEventsFromCache(): List<Event> {
        return localDatabase.getEvents()
    }

    /**
     * Save events to local cache
     */
    fun saveEventsToCache(events: List<Event>) {
        localDatabase.saveEvents(events)
    }

    /**
     * Start real-time listener for events
     */
    fun startRealtimeListener(
        onEventsChanged: (List<Event>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        eventsListener = eventsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to events", error)
                onError(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val events = snapshot.documents.mapNotNull { doc ->
                    try {
                        documentToEvent(doc.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing event: ${doc.id}", e)
                        null
                    }
                }
                saveEventsToCache(events)
                onEventsChanged(events)
            }
        }
    }

    /**
     * Stop real-time listener
     */
    fun stopRealtimeListener() {
        eventsListener?.remove()
        eventsListener = null
    }

    /**
     * Convert Event to Map for Firestore
     */
    private fun eventToMap(event: Event): Map<String, Any?> {
        return mapOf(
            "id" to event.id,
            "title" to event.title,
            "description" to event.description,
            "date" to event.date,
            "time" to event.time,
            "location" to event.location,
            "requirements" to event.requirements,
            "dresscode" to event.dresscode,
            "images" to event.images,
            "videos" to event.videos,
            "documents" to event.documents,
            "isFirewalking" to event.isFirewalking,
            "categoryId" to event.categoryId,
            "allDay" to event.allDay,
            "endDate" to event.endDate,
            "endTime" to event.endTime,
            "recurrence" to event.recurrence?.name
        )
    }

    /**
     * Convert Firestore document to Event
     */
    private fun documentToEvent(data: Map<String, Any?>?): Event? {
        if (data == null) return null

        return try {
            Event(
                id = data["id"] as? String ?: return null,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                date = (data["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                time = data["time"] as? String ?: "",
                location = data["location"] as? String ?: "",
                requirements = data["requirements"] as? String ?: "",
                dresscode = data["dresscode"] as? String ?: "",
                images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                videos = (data["videos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                documents = (data["documents"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                isFirewalking = data["isFirewalking"] as? Boolean ?: false,
                categoryId = data["categoryId"] as? String ?: "default",
                allDay = data["allDay"] as? Boolean ?: false,
                endDate = (data["endDate"] as? com.google.firebase.Timestamp)?.toDate(),
                endTime = data["endTime"] as? String?,
                recurrence = (data["recurrence"] as? String)?.let {
                    try {
                        com.example.umbilotemplefrontend.models.EventRecurrence.valueOf(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to Event", e)
            null
        }
    }
}

