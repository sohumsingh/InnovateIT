package com.example.umbilotemplefrontend.models

import java.io.Serializable
import java.util.Date

data class EventReminder(
    val id: String,
    val eventId: String,
    val reminderTime: Date,
    val notificationSent: Boolean = false
) : Serializable
