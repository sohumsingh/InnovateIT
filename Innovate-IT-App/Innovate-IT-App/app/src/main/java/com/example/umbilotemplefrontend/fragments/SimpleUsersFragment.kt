package com.example.umbilotemplefrontend.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.User
import com.example.umbilotemplefrontend.utils.LocalDatabase

/**
 * A simplified version of the UsersFragment for testing
 */
class SimpleUsersFragment : Fragment() {

    private lateinit var localDatabase: LocalDatabase
    private val users = mutableListOf<User>()
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("SimpleUsersFragment", "onCreateView started")
        
        try {
            // Create a simple TextView instead of using a layout file
            val textView = TextView(requireContext())
            textView.setPadding(32, 32, 32, 32)
            textView.textSize = 16f
            
            // Initialize database
            localDatabase = LocalDatabase(requireContext())
            
            // Get current user
            currentUser = localDatabase.getCurrentUser()
            Log.d("SimpleUsersFragment", "Current user: ${currentUser?.name}, isAdmin: ${currentUser?.isAdmin}")
            
            // Load users
            loadUsers()
            
            // Display user list
            val userList = StringBuilder()
            userList.append("Users (${users.size}):\n\n")
            
            users.forEachIndexed { index, user ->
                userList.append("${index + 1}. ${user.name}\n")
                userList.append("   Email: ${user.email}\n")
                userList.append("   Admin: ${if (user.isAdmin) "Yes" else "No"}\n\n")
            }
            
            textView.text = userList.toString()
            
            Log.d("SimpleUsersFragment", "onCreateView completed")
            return textView
            
        } catch (e: Exception) {
            Log.e("SimpleUsersFragment", "Error in onCreateView", e)
            val errorView = TextView(requireContext())
            errorView.text = "Error loading users: ${e.message}"
            return errorView
        }
    }
    
    private fun loadUsers() {
        Log.d("SimpleUsersFragment", "Loading users")
        try {
            // Get all users
            val allUsers = localDatabase.getUsers()
            Log.d("SimpleUsersFragment", "Found ${allUsers.size} users")
            
            // Filter out current user
            users.clear()
            users.addAll(allUsers.filter { it.id != currentUser?.id })
            Log.d("SimpleUsersFragment", "Filtered to ${users.size} users (excluding current user)")
        } catch (e: Exception) {
            Log.e("SimpleUsersFragment", "Error loading users", e)
        }
    }
}
