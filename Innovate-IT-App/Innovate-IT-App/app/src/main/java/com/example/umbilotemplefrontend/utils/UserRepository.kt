package com.example.umbilotemplefrontend.utils

import android.content.Context
import android.util.Log
import com.example.umbilotemplefrontend.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

/**
 * Repository for managing User data with Firebase Firestore sync
 */
class UserRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val localDatabase = LocalDatabase(context)
    private var usersListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * Fetch all users from Firestore
     */
    fun fetchUsersFromFirestore(
        onSuccess: (List<User>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        usersCollection.get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    try {
                        documentToUser(doc.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user: ${doc.id}", e)
                        null
                    }
                }
                // Cache locally
                saveUsersToCache(users)
                onSuccess(users)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching users from Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Save a single user to Firestore
     */
    fun saveUserToFirestore(
        user: User,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userMap = userToMap(user)
        usersCollection.document(user.id).set(userMap)
            .addOnSuccessListener {
                Log.d(TAG, "User saved to Firestore: ${user.id}")
                // Update local cache
                val users = localDatabase.getUsers().toMutableList()
                val existingIndex = users.indexOfFirst { it.id == user.id }
                if (existingIndex >= 0) {
                    users[existingIndex] = user
                } else {
                    users.add(user)
                }
                localDatabase.saveUsers(users)
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error saving user to Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Update an existing user in Firestore
     */
    fun updateUserInFirestore(
        user: User,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        saveUserToFirestore(user, onSuccess, onError)
    }

    /**
     * Delete a user from Firestore
     */
    fun deleteUserFromFirestore(
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        usersCollection.document(userId).delete()
            .addOnSuccessListener {
                Log.d(TAG, "User deleted from Firestore: $userId")
                // Update local cache
                val users = localDatabase.getUsers().toMutableList()
                val iterator = users.iterator()
                while (iterator.hasNext()) {
                    if (iterator.next().id == userId) {
                        iterator.remove()
                        break
                    }
                }
                localDatabase.saveUsers(users)
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting user from Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Get users from local cache
     */
    fun getUsersFromCache(): List<User> {
        return localDatabase.getUsers()
    }

    /**
     * Save users to local cache
     */
    fun saveUsersToCache(users: List<User>) {
        localDatabase.saveUsers(users)
    }

    /**
     * Start real-time listener for users
     */
    fun startRealtimeListener(
        onUsersChanged: (List<User>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        usersListener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to users", error)
                onError(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val users = snapshot.documents.mapNotNull { doc ->
                    try {
                        documentToUser(doc.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user: ${doc.id}", e)
                        null
                    }
                }
                saveUsersToCache(users)
                onUsersChanged(users)
            }
        }
    }

    /**
     * Stop real-time listener
     */
    fun stopRealtimeListener() {
        usersListener?.remove()
        usersListener = null
    }

    /**
     * Convert User to Map for Firestore
     */
    private fun userToMap(user: User): Map<String, Any?> {
        return mapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "passwordHash" to user.passwordHash,
            "phoneNumber" to user.phoneNumber,
            "registrationDate" to user.registrationDate,
            "lastLoginDate" to user.lastLoginDate,
            "profilePicturePath" to user.profilePicturePath,
            "isEmailVerified" to user.isEmailVerified,
            "isAdmin" to user.isAdmin,
            "authProvider" to user.authProvider.name
        )
    }

    /**
     * Convert Firestore document to User
     */
    private fun documentToUser(data: Map<String, Any?>?): User? {
        if (data == null) return null

        return try {
            User(
                id = data["id"] as? String ?: return null,
                name = data["name"] as? String ?: "",
                email = data["email"] as? String ?: "",
                passwordHash = data["passwordHash"] as? String ?: "",
                phoneNumber = data["phoneNumber"] as? String ?: "",
                registrationDate = (data["registrationDate"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                lastLoginDate = (data["lastLoginDate"] as? com.google.firebase.Timestamp)?.toDate(),
                profilePicturePath = data["profilePicturePath"] as? String,
                isEmailVerified = data["isEmailVerified"] as? Boolean ?: false,
                isAdmin = data["isAdmin"] as? Boolean ?: false,
                authProvider = (data["authProvider"] as? String)?.let {
                    try {
                        User.AuthProvider.valueOf(it)
                    } catch (e: Exception) {
                        User.AuthProvider.EMAIL
                    }
                } ?: User.AuthProvider.EMAIL
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to User", e)
            null
        }
    }
}

