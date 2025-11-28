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
 * Simple test activity to verify admin navigation
 */
class AdminTestActivity : AppCompatActivity() {

    private lateinit var localDatabase: LocalDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AdminTestActivity", "onCreate started")
        
        try {
            setContentView(R.layout.activity_admin_test)
            
            // Header title and back
            findViewById<TextView>(R.id.tvHeaderTitle)?.text = "ADMIN TEST"
            findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
                // Navigate to home page instead of closing app
                val intent = android.content.Intent(this, HomeActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            
            Log.d("AdminTestActivity", "setContentView completed")
            
            // Initialize database
            localDatabase = LocalDatabase(this)
            
            // Get current user
            val currentUser = localDatabase.getCurrentUser()
            Log.d("AdminTestActivity", "Current user: ${currentUser?.name}, isAdmin: ${currentUser?.isAdmin}")
            
            Toast.makeText(this, "Admin Test Activity Loaded", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("AdminTestActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
