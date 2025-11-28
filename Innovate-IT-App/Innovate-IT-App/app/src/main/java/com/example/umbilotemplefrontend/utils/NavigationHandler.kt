package com.example.umbilotemplefrontend.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.umbilotemplefrontend.AdminActivity
import com.example.umbilotemplefrontend.BookingsActivity
import com.example.umbilotemplefrontend.CalendarActivity
import com.example.umbilotemplefrontend.DonationsActivity
import com.example.umbilotemplefrontend.DocumentsActivity
import com.example.umbilotemplefrontend.GalleryActivity
import com.example.umbilotemplefrontend.EventsActivity
import com.example.umbilotemplefrontend.HomeActivity
import com.example.umbilotemplefrontend.LoginActivity
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Utility class to handle navigation between activities
 */
class NavigationHandler(private val activity: Activity) {
    
    private val localDatabase: LocalDatabase by lazy {
        LocalDatabase(activity)
    }

    /**
     * Sets up the navigation bar with click listeners
     */
    fun setupNavigation() {
        val bottomNavigation = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        
        if (bottomNavigation == null) {
            Log.e("NavigationHandler", "BottomNavigationView not found in ${activity.javaClass.simpleName}")
            return
        }
        
        Log.d("NavigationHandler", "Setting up navigation for ${activity.javaClass.simpleName}")
        
        // Set the selected item FIRST to avoid triggering the listener
        val targetItemId = when (activity) {
            is HomeActivity -> R.id.nav_home
            is EventsActivity -> R.id.nav_events
            is CalendarActivity -> R.id.nav_calendar
            is BookingsActivity -> R.id.nav_bookings
            is SettingsActivity -> R.id.nav_settings
            is AdminActivity -> R.id.nav_home
            is GalleryActivity -> R.id.nav_home
            is DocumentsActivity -> R.id.nav_home
            else -> R.id.nav_home
        }
        // Only set if item exists to avoid crashes when menus differ
        if (bottomNavigation.menu.findItem(targetItemId) != null) {
            bottomNavigation.selectedItemId = targetItemId
        } else {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
        
        // THEN set the listener
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            Log.d("NavigationHandler", "Menu item selected: ${menuItem.title}")
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (activity !is HomeActivity) {
                        activity.startActivity(Intent(activity, HomeActivity::class.java))
                        activity.finish()
                    }
                    true
                }
                R.id.nav_events -> {
                    if (activity !is EventsActivity) {
                        activity.startActivity(Intent(activity, EventsActivity::class.java))
                        activity.finish()
                    }
                    true
                }
                R.id.nav_calendar -> {
                    if (activity !is CalendarActivity) {
                        activity.startActivity(Intent(activity, CalendarActivity::class.java))
                        activity.finish()
                    }
                    true
                }
                R.id.nav_bookings -> {
                    if (activity !is BookingsActivity) {
                        activity.startActivity(Intent(activity, BookingsActivity::class.java))
                        activity.finish()
                    }
                    true
                }
                R.id.nav_settings -> {
                    if (activity !is SettingsActivity) {
                        activity.startActivity(Intent(activity, SettingsActivity::class.java))
                        activity.finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Confirms if the user wants to logout
     */
    private fun confirmLogout() {
        AlertDialog.Builder(activity)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    /**
     * Logs out the user and navigates to the login screen
     */
    private fun logout() {
        // Clear current user session
        localDatabase.clearCurrentUser()
        
        // Navigate back to login screen
        activity.startActivity(Intent(activity, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        activity.finish()
    }
    
    /**
     * Checks if the user is logged in and redirects to login if not
     * @return true if user is logged in, false otherwise
     */
    fun checkUserLoggedIn(): Boolean {
        if (!localDatabase.isLoggedIn()) {
            // Redirect to login screen
            activity.startActivity(Intent(activity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            activity.finish()
            return false
        }
        return true
    }
} 