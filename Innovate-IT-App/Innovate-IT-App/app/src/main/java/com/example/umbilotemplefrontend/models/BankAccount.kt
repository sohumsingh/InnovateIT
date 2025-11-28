package com.example.umbilotemplefrontend.models

import java.io.Serializable

/**
 * Model class for Bank Accounts used for donations
 */
data class BankAccount(
    val id: String,
    val bankName: String,
    val accountNumber: String,
    val branchCode: String,
    val iconResId: Int = 0 // Resource ID for bank icon/logo
) : Serializable

