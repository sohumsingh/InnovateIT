package com.example.umbilotemplefrontend.models

import java.io.Serializable
import java.util.Date

/**
 * Model class for User
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String, // Store hashed password, not plain text
    val phoneNumber: String = "",
    val registrationDate: Date = Date(),
    val lastLoginDate: Date? = null,
    val profilePicturePath: String? = null,
    val isEmailVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val authProvider: AuthProvider = AuthProvider.EMAIL
) : Serializable {
    enum class AuthProvider {
        EMAIL, GOOGLE, FACEBOOK
    }
}
