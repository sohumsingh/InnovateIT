package com.example.umbilotemplefrontend.models

import java.io.Serializable
import java.util.Date

/**
 * Model class for Events
 */
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val date: Date,
    val time: String,
    val location: String,
    val requirements: String,
    val dresscode: String,
    val images: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    val documents: List<String> = emptyList(),
    val isFirewalking: Boolean = false,
    val categoryId: String = EventCategory.DEFAULT.id,
    val reminders: List<EventReminder> = emptyList(),
    val allDay: Boolean = false,
    val endDate: Date? = null,
    val endTime: String? = null,
    val recurrence: EventRecurrence? = null
) : Serializable {
    
    fun getCategory(): EventCategory {
        return EventCategory.getById(categoryId)
    }
    
    fun isMultiDay(): Boolean {
        return endDate != null && endDate != date
    }
}

/**
 * Enum for event recurrence patterns
 */
enum class EventRecurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}