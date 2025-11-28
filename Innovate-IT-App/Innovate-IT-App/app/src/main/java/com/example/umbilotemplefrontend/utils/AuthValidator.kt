package com.example.umbilotemplefrontend.utils

object AuthValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    private val PASSWORD_REGEX = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$")
    private val NAME_REGEX = Regex("^[a-zA-Z\\s]{2,30}$")
    private val PHONE_REGEX = Regex("^\\+?[0-9]{10,15}$")

    fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() && EMAIL_REGEX.matches(email)
    }

    fun isPasswordValid(password: String): Boolean {
        // Password must be at least 8 characters with at least one digit, one uppercase, one lowercase, and one special character
        return password.isNotBlank() && PASSWORD_REGEX.matches(password)
    }

    fun isPasswordValidBasic(password: String): Boolean {
        // Simpler validation for minimum length
        return password.length >= 8
    }

    fun isNameValid(name: String): Boolean {
        return name.trim().isNotEmpty() && NAME_REGEX.matches(name.trim())
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
    
    fun isPhoneNumberValid(phoneNumber: String): Boolean {
        return phoneNumber.isBlank() || PHONE_REGEX.matches(phoneNumber.trim())
    }
    
    fun getPasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) {
            return PasswordStrength.WEAK
        }
        
        var score = 0
        
        // Check for digit
        if (password.any { it.isDigit() }) score++
        
        // Check for lowercase
        if (password.any { it.isLowerCase() }) score++
        
        // Check for uppercase
        if (password.any { it.isUpperCase() }) score++
        
        // Check for special character
        if (password.any { !it.isLetterOrDigit() }) score++
        
        // Check for length
        if (password.length >= 12) score++
        
        return when (score) {
            0, 1 -> PasswordStrength.WEAK
            2, 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }
    
    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG
    }
    
    fun getPasswordErrorMessage(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isDigit() } -> "Password must contain at least one digit"
            !password.any { it.isLowerCase() } -> "Password must contain at least one lowercase letter"
            !password.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter"
            !password.any { !it.isLetterOrDigit() } -> "Password must contain at least one special character"
            else -> null
        }
    }
}
