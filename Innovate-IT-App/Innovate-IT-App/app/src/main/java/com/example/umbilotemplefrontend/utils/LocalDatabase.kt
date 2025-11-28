package com.example.umbilotemplefrontend.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.umbilotemplefrontend.models.BankAccount
import com.example.umbilotemplefrontend.models.Booking
import com.example.umbilotemplefrontend.models.Document
import com.example.umbilotemplefrontend.models.Donation
import com.example.umbilotemplefrontend.models.Event
import com.example.umbilotemplefrontend.models.User
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * LocalDatabase helper class that saves data to text files as a backup
 * This is a temporary solution until a proper database is implemented
 */
class LocalDatabase(private val context: Context) {

    companion object {
        private const val TAG = "LocalDatabase"
        private const val EVENTS_FILE = "events.txt"
        private const val DOCUMENTS_FILE = "documents.txt"
        private const val DONATIONS_FILE = "donations.txt"
        private const val USERS_FILE = "users.txt"
        private const val BANK_ACCOUNTS_FILE = "bank_accounts.txt"
        private const val BOOKINGS_FILE = "bookings.txt"
        private const val FAQ_UNMATCHED_FILE = "faq_unmatched.txt"
        private const val PREFS_NAME = "UmbiloTemplePrefs"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    // Shared preferences for session management
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Directory for the database files
    private val databaseDir: File by lazy {
        File(context.filesDir, "local_database").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    // Generic method to save any serializable data to a file
    fun <T> saveData(data: T, filename: String) {
        try {
            val file = File(databaseDir, filename)
            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(data)
                }
            }
            Log.d(TAG, "Data saved successfully to $filename")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving data to $filename", e)
            // Create a backup with timestamp in case of error
            createBackup(filename)
        }
    }

    // Generic method to read any serializable data from a file
    @Suppress("UNCHECKED_CAST")
    fun <T> readData(filename: String): T? {
        try {
            val file = File(databaseDir, filename)
            if (!file.exists()) {
                Log.d(TAG, "File $filename does not exist")
                return null
            }

            FileInputStream(file).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    return ois.readObject() as T
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading data from $filename", e)
            // Try to recover from backup if available
            return recoverFromBackup(filename) as? T
        }
    }

    // Create a backup of a file with timestamp
    private fun createBackup(filename: String) {
        try {
            val sourceFile = File(databaseDir, filename)
            if (!sourceFile.exists()) return

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(databaseDir, "${filename}_backup_$timestamp")
            
            sourceFile.copyTo(backupFile, overwrite = true)
            Log.d(TAG, "Backup created: ${backupFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup for $filename", e)
        }
    }

    // Try to recover data from the most recent backup
    private fun recoverFromBackup(filename: String): Any? {
        try {
            val backupFiles = databaseDir.listFiles { file ->
                file.name.startsWith("${filename}_backup_")
            }

            val mostRecentBackup = backupFiles?.maxByOrNull { it.lastModified() }
            if (mostRecentBackup != null) {
                Log.d(TAG, "Attempting recovery from ${mostRecentBackup.name}")
                FileInputStream(mostRecentBackup).use { fis ->
                    ObjectInputStream(fis).use { ois ->
                        return ois.readObject()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recover from backup for $filename", e)
        }
        return null
    }

    // Events specific methods
    fun saveEvents(events: List<Event>) {
        saveData(events, EVENTS_FILE)
    }

    fun getEvents(): List<Event> {
        return try {
            readData<List<Event>>(EVENTS_FILE) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading events, returning empty list", e)
            emptyList()
        }
    }

    // Documents specific methods
    fun saveDocuments(documents: List<Document>) {
        saveData(documents, DOCUMENTS_FILE)
    }

    fun getDocuments(): List<Document> {
        return try {
            readData<List<Document>>(DOCUMENTS_FILE) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading documents, deleting corrupted file and returning empty list", e)
            try {
                // Delete corrupted file to prevent future crashes
                File(databaseDir, DOCUMENTS_FILE).delete()
                Log.d(TAG, "Deleted corrupted documents file")
            } catch (deleteError: Exception) {
                Log.e(TAG, "Error deleting corrupted documents file", deleteError)
            }
            emptyList()
        }
    }

    // Donations specific methods
    fun saveDonations(donations: List<Donation>) {
        saveData(donations, DONATIONS_FILE)
    }

    fun getDonations(): List<Donation> {
        return try {
            readData<List<Donation>>(DONATIONS_FILE) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading donations, returning empty list", e)
            emptyList()
        }
    }
    
    // User specific methods
    fun saveUsers(users: List<User>) {
        // CRITICAL FIX: Ensure admin email always retains admin privileges when saving
        val fixedUsers = users.map { user ->
            if (user.email.equals("TestingAdmin@gmail.com", ignoreCase = true) && !user.isAdmin) {
                Log.w(TAG, "Fixing admin status for ${user.email} when saving users")
                user.copy(isAdmin = true)
            } else {
                user
            }
        }
        saveData(fixedUsers, USERS_FILE)
    }
    
    fun getUsers(): List<User> {
        return try {
            readData<List<User>>(USERS_FILE) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading users, returning empty list", e)
            emptyList()
        }
    }
    
    fun getUserByEmail(email: String): User? {
        return getUsers().find { it.email.equals(email, ignoreCase = true) }
    }
    
    fun getUserById(id: String): User? {
        var user = getUsers().find { it.id == id }
        
        // CRITICAL FIX: Ensure TestingAdmin@gmail.com always has admin privileges
        if (user != null && user.email.equals("TestingAdmin@gmail.com", ignoreCase = true) && !user.isAdmin) {
            Log.w(TAG, "Admin user found but lost privileges! Restoring admin status for user: ${user.email}")
            user = user.copy(isAdmin = true)
            updateUser(user)
        }
        
        return user
    }
    
    fun registerUser(name: String, email: String, password: String, phoneNumber: String = "", isAdmin: Boolean = false): User? {
        val users = getUsers().toMutableList()
        
        // Check if user already exists
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            return null
        }
        
        // Create new user
        val newUser = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            passwordHash = hashPassword(password),
            phoneNumber = phoneNumber,
            registrationDate = Date(),
            isAdmin = isAdmin
        )
        
        users.add(newUser)
        saveUsers(users)
        return newUser
    }
    
    // Initialize default admin user if none exists
    fun initializeDefaultAdminIfNeeded(): Boolean {
        val users = getUsers()
        
        // Check if admin user exists
        if (!users.any { it.isAdmin }) {
            // Create default admin user
            val adminUser = registerUser(
                name = "Testing",
                email = "TestingAdmin@gmail.com",
                password = "Testing@1",
                phoneNumber = "066 025 1636",
                isAdmin = true
            )
            return adminUser != null
        }
        return false
    }
    
    fun authenticateUser(email: String, password: String): User? {
        // Special case for admin user
        if (email.equals("TestingAdmin@gmail.com", ignoreCase = true) && password == "Testing@1") {
            Log.d(TAG, "Admin login attempt detected")
            
            // Check if admin user exists
            var adminUser = getUserByEmail(email)
            
            // If not, create it
            if (adminUser == null) {
                Log.d(TAG, "Admin user not found, creating it")
                adminUser = registerUser(
                    name = "Testing",
                    email = "TestingAdmin@gmail.com",
                    password = "Testing@1",
                    phoneNumber = "066 025 1636",
                    isAdmin = true
                )
                Log.d(TAG, "Admin user created: $adminUser")
            } else {
                // Ensure it has admin privileges
                if (!adminUser.isAdmin) {
                    Log.d(TAG, "Updating user to have admin privileges")
                    adminUser = adminUser.copy(isAdmin = true)
                    updateUser(adminUser)
                }
            }
            
            return adminUser
        }
        
        // Regular authentication
        val user = getUserByEmail(email) ?: return null
        
        // Check password
        if (user.passwordHash == hashPassword(password)) {
            // Update last login date
            val updatedUser = user.copy(lastLoginDate = Date())
            updateUser(updatedUser)
            return updatedUser
        }
        
        return null
    }
    
    fun updateUser(user: User): Boolean {
        val users = getUsers().toMutableList()
        val index = users.indexOfFirst { it.id == user.id }
        
        if (index == -1) return false
        
        users[index] = user
        saveUsers(users)
        return true
    }
    
    fun deleteUser(userId: String): Boolean {
        val users = getUsers().toMutableList()
        val iterator = users.iterator()
        var removed = false
        
        while (iterator.hasNext()) {
            if (iterator.next().id == userId) {
                iterator.remove()
                removed = true
                break
            }
        }
        
        if (removed) {
            saveUsers(users)
            if (getCurrentUserId() == userId) {
                clearCurrentUser()
            }
        }
        
        return removed
    }
    
    // Session management
    fun setCurrentUser(user: User) {
        sharedPreferences.edit().putString(KEY_CURRENT_USER_ID, user.id).apply()
    }
    
    fun getCurrentUser(): User? {
        val userId = getCurrentUserId() ?: return null
        var user = getUserById(userId)
        
        // CRITICAL FIX: Always ensure TestingAdmin@gmail.com has admin privileges
        if (user != null && user.email.equals("TestingAdmin@gmail.com", ignoreCase = true)) {
            if (!user.isAdmin) {
                Log.w(TAG, "Admin user lost privileges in getCurrentUser! Restoring...")
                user = user.copy(isAdmin = true)
                updateUser(user)
            }
        }
        
        // Debug logging
        if (user != null) {
            Log.d(TAG, "Retrieved current user: ${user.email}, isAdmin: ${user.isAdmin}")
        } else {
            Log.e(TAG, "Current user not found with ID: $userId")
        }
        
        return user
    }
    
    fun getCurrentUserId(): String? {
        return sharedPreferences.getString(KEY_CURRENT_USER_ID, null)
    }
    
    fun clearCurrentUser() {
        sharedPreferences.edit().remove(KEY_CURRENT_USER_ID).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return getCurrentUserId() != null
    }

    // ==================== FAQ Unmatched Query Logging ====================
    /**
     * Append an unmatched FAQ query entry. We keep a rolling list (max 200 entries).
     * Entry format: ISO_TIMESTAMP|USER_ID|IS_ADMIN|QUERY
     */
    fun addFaqUnmatched(entry: String) {
        try {
            val existing = readData<List<String>>(FAQ_UNMATCHED_FILE) ?: emptyList()
            val updated = (existing + entry).takeLast(200)
            saveData(updated, FAQ_UNMATCHED_FILE)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding FAQ unmatched entry", e)
        }
    }

    fun getFaqUnmatched(): List<String> {
        return try {
            readData<List<String>>(FAQ_UNMATCHED_FILE) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading FAQ unmatched entries", e)
            emptyList()
        }
    }
    
    // Password hashing (simple implementation - in a real app, use a more secure method)
    private fun hashPassword(password: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray())
            return hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error hashing password", e)
            // Fallback to simple encoding if hashing fails
            return password.reversed()
        }
    }
    
    // ==================== Bank Accounts Methods ====================
    
    /**
     * Get all bank accounts, initialize with default if empty
     */
    fun getBankAccounts(): List<BankAccount> {
        val accounts = try {
            readData<List<BankAccount>>(BANK_ACCOUNTS_FILE)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading bank accounts, returning default", e)
            null
        }
        
        // If no accounts exist, initialize with default Nedbank account
        if (accounts == null || accounts.isEmpty()) {
            val defaultAccounts = listOf(
                BankAccount(
                    id = UUID.randomUUID().toString(),
                    bankName = "Nedbank",
                    accountNumber = "1304036316",
                    branchCode = "198765"
                )
            )
            saveBankAccounts(defaultAccounts)
            return defaultAccounts
        }
        
        return accounts
    }
    
    /**
     * Save all bank accounts
     */
    fun saveBankAccounts(accounts: List<BankAccount>) {
        saveData(accounts, BANK_ACCOUNTS_FILE)
    }
    
    /**
     * Add a new bank account
     */
    fun addBankAccount(account: BankAccount): Boolean {
        return try {
            val accounts = getBankAccounts().toMutableList()
            accounts.add(account)
            saveBankAccounts(accounts)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bank account", e)
            false
        }
    }
    
    /**
     * Update an existing bank account
     */
    fun updateBankAccount(account: BankAccount): Boolean {
        return try {
            val accounts = getBankAccounts().toMutableList()
            val index = accounts.indexOfFirst { it.id == account.id }
            
            if (index != -1) {
                accounts[index] = account
                saveBankAccounts(accounts)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bank account", e)
            false
        }
    }
    
    /**
     * Delete a bank account by ID
     */
    fun deleteBankAccount(accountId: String): Boolean {
        return try {
            val accounts = getBankAccounts().toMutableList()
            val iterator = accounts.iterator()
            var removed = false
            
            while (iterator.hasNext()) {
                if (iterator.next().id == accountId) {
                    iterator.remove()
                    removed = true
                    break
                }
            }
            
            if (removed) {
                saveBankAccounts(accounts)
            }
            
            removed
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting bank account", e)
            false
        }
    }
    
    /**
     * Get a bank account by ID
     */
    fun getBankAccountById(accountId: String): BankAccount? {
        return getBankAccounts().firstOrNull { it.id == accountId }
    }
    
    // ==================== Bookings Methods ====================
    
    /**
     * Get all bookings
     */
    fun getBookings(): List<Booking> {
        return try {
            readData<List<Booking>>(BOOKINGS_FILE) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading bookings, returning empty list", e)
            emptyList()
        }
    }
    
    /**
     * Get bookings for a specific user
     */
    fun getBookingsByUserId(userId: String): List<Booking> {
        return getBookings().filter { it.userId == userId }
    }
    
    /**
     * Get bookings by status
     */
    fun getBookingsByStatus(status: com.example.umbilotemplefrontend.models.BookingStatus): List<Booking> {
        return getBookings().filter { it.status == status }
    }
    
    /**
     * Save all bookings
     */
    fun saveBookings(bookings: List<Booking>) {
        saveData(bookings, BOOKINGS_FILE)
    }
    
    /**
     * Add a new booking
     */
    fun addBooking(booking: Booking): Boolean {
        return try {
            val bookings = getBookings().toMutableList()
            bookings.add(booking)
            saveBookings(bookings)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding booking", e)
            false
        }
    }
    
    /**
     * Update an existing booking
     */
    fun updateBooking(booking: Booking): Boolean {
        return try {
            val bookings = getBookings().toMutableList()
            val index = bookings.indexOfFirst { it.id == booking.id }
            
            if (index != -1) {
                bookings[index] = booking
                saveBookings(bookings)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating booking", e)
            false
        }
    }
    
    /**
     * Delete a booking by ID
     */
    fun deleteBooking(bookingId: String): Boolean {
        return try {
            val bookings = getBookings().toMutableList()
            val iterator = bookings.iterator()
            var removed = false
            
            while (iterator.hasNext()) {
                if (iterator.next().id == bookingId) {
                    iterator.remove()
                    removed = true
                    break
                }
            }
            
            if (removed) {
                saveBookings(bookings)
            }
            
            removed
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting booking", e)
            false
        }
    }
    
    /**
     * Get a booking by ID
     */
    fun getBookingById(bookingId: String): Booking? {
        return getBookings().firstOrNull { it.id == bookingId }
    }
} 