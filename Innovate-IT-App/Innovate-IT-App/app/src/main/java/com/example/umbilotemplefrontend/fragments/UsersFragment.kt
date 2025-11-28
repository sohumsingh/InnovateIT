package com.example.umbilotemplefrontend.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.adapters.UsersAdapter
import com.example.umbilotemplefrontend.models.User
import com.example.umbilotemplefrontend.utils.LocalDatabase

class UsersFragment : Fragment(), UsersAdapter.UserActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var localDatabase: LocalDatabase
    private val users = mutableListOf<User>()
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_users, container, false)
        
        // Initialize database
        localDatabase = LocalDatabase(requireContext())
        
        // Get current user
        currentUser = localDatabase.getCurrentUser()
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.usersRecyclerView)
        usersAdapter = UsersAdapter(requireContext(), users, this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = usersAdapter
        
        // Load users
        loadUsers()
        
        return view
    }
    
    private fun loadUsers() {
        // Get all users
        val allUsers = localDatabase.getUsers()
        
        // Filter out current user
        users.clear()
        users.addAll(allUsers.filter { it.id != currentUser?.id })
        usersAdapter.notifyDataSetChanged()
    }
    
    override fun onToggleAdminStatus(user: User, position: Int) {
        // Check if current user is admin
        if (currentUser?.isAdmin != true) {
            Toast.makeText(requireContext(), "You don't have admin privileges", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Toggle admin status
        val updatedUser = user.copy(isAdmin = !user.isAdmin)
        
        // Update user in database
        if (localDatabase.updateUser(updatedUser)) {
            // Update local list
            users[position] = updatedUser
            usersAdapter.notifyItemChanged(position)
            
            // Show success message
            val message = if (updatedUser.isAdmin) {
                "${user.name} is now an admin"
            } else {
                "${user.name} is no longer an admin"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Failed to update user", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDeleteUser(user: User, position: Int) {
        // Check if current user is admin
        if (currentUser?.isAdmin != true) {
            Toast.makeText(requireContext(), "You don't have admin privileges", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Confirm deletion
        AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete user from database
                if (localDatabase.deleteUser(user.id)) {
                    // Remove from local list
                    users.removeAt(position)
                    usersAdapter.notifyItemRemoved(position)
                    Toast.makeText(requireContext(), "User deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete user", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onViewUserDocuments(user: User, position: Int) {
        // This functionality is primarily in AdminActivity
        // For the fragment, show user info
        Toast.makeText(requireContext(), "View ${user.name}'s documents from Admin Panel", Toast.LENGTH_SHORT).show()
    }
}
