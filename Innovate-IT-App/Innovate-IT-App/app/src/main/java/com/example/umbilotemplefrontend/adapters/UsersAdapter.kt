package com.example.umbilotemplefrontend.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.User
import java.text.SimpleDateFormat
import java.util.Locale

class UsersAdapter(
    private val context: Context,
    private var users: List<User>,
    private val listener: UserActionListener,
    private val isDocumentsMode: Boolean = false
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    interface UserActionListener {
        fun onToggleAdminStatus(user: User, position: Int)
        fun onDeleteUser(user: User, position: Int)
        fun onViewUserDocuments(user: User, position: Int)
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvRegistrationDate: TextView = view.findViewById(R.id.tvRegistrationDate)
        val tvAdminStatus: TextView = view.findViewById(R.id.tvAdminStatus)
        val btnToggleAdmin: Button = view.findViewById(R.id.btnToggleAdmin)
        val btnDeleteUser: Button = view.findViewById(R.id.btnDeleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        
        holder.tvUserName.text = user.name
        holder.tvUserEmail.text = user.email
        holder.tvRegistrationDate.text = "Registered: ${dateFormat.format(user.registrationDate)}"
        
        // Hide buttons if in Documents mode
        if (isDocumentsMode) {
            holder.btnToggleAdmin.visibility = View.GONE
            holder.btnDeleteUser.visibility = View.GONE
        } else {
            holder.btnToggleAdmin.visibility = View.VISIBLE
            holder.btnDeleteUser.visibility = View.VISIBLE
            // Set admin status
            holder.tvAdminStatus.text = if (user.isAdmin) "Admin" else "User"
            holder.btnToggleAdmin.text = if (user.isAdmin) "Remove Admin" else "Make Admin"
            
            // Set button click listeners
            holder.btnToggleAdmin.setOnClickListener {
                listener.onToggleAdminStatus(user, position)
            }
            
            holder.btnDeleteUser.setOnClickListener {
                listener.onDeleteUser(user, position)
            }
        }
        
        // Make user name clickable to view their documents
        holder.tvUserName.setOnClickListener {
            listener.onViewUserDocuments(user, position)
        }
    }

    fun updateUsers(newUsers: List<User>) {
        this.users = newUsers
        notifyDataSetChanged()
    }
    
    fun getUsers(): List<User> {
        return users
    }
}
