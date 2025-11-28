package com.example.umbilotemplefrontend

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.umbilotemplefrontend.fragments.SimpleEventsFragment
import com.example.umbilotemplefrontend.fragments.SimpleUsersFragment
import com.example.umbilotemplefrontend.models.User
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * A simplified version of the AdminActivity that uses simpler fragments
 */
class AdminSimpleFragmentActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var localDatabase: LocalDatabase
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AdminSimpleFrag", "onCreate started")
        
        try {
            setContentView(R.layout.activity_admin_simple)
            Log.d("AdminSimpleFrag", "setContentView completed")
            
            // Set up back button
            val btnBack = findViewById<Button>(R.id.btnBack)
            btnBack.setOnClickListener {
                // Navigate to home page instead of closing app
                val intent = android.content.Intent(this, HomeActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            
            // Initialize database
            localDatabase = LocalDatabase(this)
            Log.d("AdminSimpleFrag", "LocalDatabase initialized")
            
            // Get current user
            currentUser = localDatabase.getCurrentUser()
            Log.d("AdminSimpleFrag", "Current user: ${currentUser?.name}, isAdmin: ${currentUser?.isAdmin}")
            
            // Check if user is admin
            if (currentUser?.isAdmin != true) {
                Toast.makeText(this, "You don't have admin privileges", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Initialize views
            viewPager = findViewById(R.id.contentFrame)
            tabLayout = findViewById(R.id.tabLayout)
            
            // Set up ViewPager with fragments
            setupViewPager()
            
            Toast.makeText(this, "Admin Fragment Activity Loaded", Toast.LENGTH_SHORT).show()
            Log.d("AdminSimpleFrag", "onCreate completed successfully")
            
        } catch (e: Exception) {
            Log.e("AdminSimpleFrag", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupViewPager() {
        Log.d("AdminSimpleFrag", "Setting up ViewPager")
        
        try {
            // Set up adapter
            val adapter = AdminPagerAdapter(this)
            viewPager.adapter = adapter
            Log.d("AdminSimpleFrag", "Adapter set")
            
            // Connect TabLayout with ViewPager2
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Users"
                    1 -> "Events"
                    else -> ""
                }
            }.attach()
            Log.d("AdminSimpleFrag", "TabLayoutMediator attached")
            
        } catch (e: Exception) {
            Log.e("AdminSimpleFrag", "Error setting up ViewPager", e)
            throw e
        }
    }
    
    inner class AdminPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            Log.d("AdminSimpleFrag", "Creating fragment for position $position")
            return when (position) {
                0 -> SimpleUsersFragment()
                1 -> SimpleEventsFragment()
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }
}
