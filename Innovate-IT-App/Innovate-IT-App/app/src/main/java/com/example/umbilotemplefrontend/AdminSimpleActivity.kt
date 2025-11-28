package com.example.umbilotemplefrontend

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.umbilotemplefrontend.utils.LocalDatabase

/**
 * Simplified version of the AdminActivity with minimal UI
 */
class AdminSimpleActivity : AppCompatActivity() {

    private lateinit var localDatabase: LocalDatabase
    private var currentUser: com.example.umbilotemplefrontend.models.User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AdminSimpleActivity", "onCreate started")
        
        try {
            setContentView(R.layout.activity_admin_simple)
            
            // Set header title and back button
            findViewById<TextView>(R.id.tvHeaderTitle)?.text = "ADMIN PANEL"
            val btnBack = findViewById<ImageButton>(R.id.btnBack)
            btnBack.setOnClickListener {
                // Navigate to home page instead of closing app
                val intent = android.content.Intent(this, HomeActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            
            // Set up TabLayout
            val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
            val tvContent = findViewById<TextView>(R.id.tvContent)
            
            tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> tvContent.text = "Users Tab Selected\n\nThis would show a list of users."
                        1 -> tvContent.text = "Events Tab Selected\n\nThis would show a list of events."
                    }
                }
                
                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
                
                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            })
            
            // Initialize database
            localDatabase = LocalDatabase(this)
            
            // Get current user
            currentUser = localDatabase.getCurrentUser()
            Log.d("AdminSimpleActivity", "Current user: ${currentUser?.name}, isAdmin: ${currentUser?.isAdmin}")
            
            // Check if user is admin
            if (currentUser?.isAdmin != true) {
                Toast.makeText(this, "You don't have admin privileges", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Display user info in content area
            val userInfo = "Admin: ${currentUser?.name}\nEmail: ${currentUser?.email}"
            tvContent.text = "Users Tab Selected\n\n$userInfo\n\nThis would show a list of users."
            
            Toast.makeText(this, "Simple Admin Activity Loaded", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e("AdminSimpleActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
