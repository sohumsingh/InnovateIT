package com.example.umbilotemplefrontend.models

import java.io.Serializable
import java.util.Date

/**
 * Model class for Donations
 */
data class Donation(
    val id: String,
    val donorName: String,
    val amount: Double,
    val date: Date,
    val purpose: String,
    val receiptNumber: String,
    val paymentMethod: String,
    val isAnonymous: Boolean = false
) : Serializable 