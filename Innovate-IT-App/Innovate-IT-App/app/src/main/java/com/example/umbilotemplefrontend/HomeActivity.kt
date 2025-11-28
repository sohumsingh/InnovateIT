package com.example.umbilotemplefrontend

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.widget.Toast
import com.example.umbilotemplefrontend.DocumentsActivity
import com.example.umbilotemplefrontend.GalleryActivity
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.DocumentRepository
import com.example.umbilotemplefrontend.utils.EventRepository
import com.example.umbilotemplefrontend.models.Document
import com.example.umbilotemplefrontend.models.Event
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import android.os.Handler
import android.os.Looper
import com.example.umbilotemplefrontend.utils.NavigationHandler
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat

class HomeActivity : AppCompatActivity() {
    
    private lateinit var cardEvents: CardView
    private lateinit var cardDonate: CardView
    private lateinit var cardGallery: CardView
    private lateinit var cardDocuments: CardView
    private lateinit var cardLogout: CardView
    private lateinit var cardAdmin: CardView
    private lateinit var tvWelcomeUser: TextView
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var localDatabase: LocalDatabase
    private lateinit var documentRepository: DocumentRepository
    private lateinit var eventRepository: EventRepository
    private var latestRv: RecyclerView? = null
    private var latestLm: LinearLayoutManager? = null
    private var latestCarouselHandler: Handler? = null
    private var latestCarouselRunnable: Runnable? = null
    
    override fun onResume() {
        super.onResume()
        
        // Only check if views are initialized
        if (!::cardAdmin.isInitialized) {
            return
        }
        
        // Admin card should not be shown on Home; accessed via drawer only
        cardAdmin.visibility = View.GONE

        // Restart carousel if we have multiple items
        if ((latestRv?.adapter?.itemCount ?: 0) > 1) {
            startLatestCarousel()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Initialize database
        localDatabase = LocalDatabase(this)
        documentRepository = DocumentRepository(this)
        eventRepository = EventRepository(this)
        
        // Initialize default admin user if needed
        val adminCreated = localDatabase.initializeDefaultAdminIfNeeded()
        if (adminCreated) {
            Toast.makeText(this, "Admin user created: TestingAdmin@gmail.com / Testing@1", Toast.LENGTH_LONG).show()
        }
        
        // Check if user is logged in
        if (!localDatabase.isLoggedIn()) {
            // Redirect to login screen
            navigateToLogin()
            return
        }
        
        // Initialize card views
        cardEvents = findViewById(R.id.cardEvents)
        cardDonate = findViewById(R.id.cardDonate)
        cardGallery = findViewById(R.id.cardGallery)
        cardDocuments = findViewById(R.id.cardDocuments)
        cardLogout = findViewById(R.id.cardLogout)
        cardAdmin = findViewById(R.id.cardAdmin)
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)
        
        // Set welcome message with user's name
        var currentUser = localDatabase.getCurrentUser()
        
        if (currentUser != null) {
            // Debug logging to track admin status
            Log.d("HomeActivity", "Current user: ${currentUser.email}")
            Log.d("HomeActivity", "Is Admin before fix: ${currentUser.isAdmin}")
            Log.d("HomeActivity", "User ID: ${currentUser.id}")
            
            // CRITICAL FIX: Ensure admin email always has admin privileges
            if (currentUser.email.equals("TestingAdmin@gmail.com", ignoreCase = true) && !currentUser.isAdmin) {
                Log.w("HomeActivity", "Admin user lost privileges! Restoring...")
                currentUser = currentUser.copy(isAdmin = true)
                localDatabase.updateUser(currentUser)
                localDatabase.setCurrentUser(currentUser)
                Log.d("HomeActivity", "Admin privileges restored!")
            }
            
            tvWelcomeUser.text = "Welcome, ${currentUser.name}"
            
            // Debug logging after potential fix
            Log.d("HomeActivity", "Is Admin after fix: ${currentUser.isAdmin}")
            
            // Admin card is hidden; access via drawer only
            cardAdmin.visibility = View.GONE
        } else {
            Log.e("HomeActivity", "Current user is null!")
        }
        
        // Set up centralized navigation like other pages
        navigationHandler = NavigationHandler(this)
        if (!navigationHandler.checkUserLoggedIn()) {
            return
        }
        navigationHandler.setupNavigation()
        // Drawer setup
        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navigationView)
        findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            drawer?.openDrawer(GravityCompat.END)
        }
        // Show Admin item only for admins
        try {
            val current = localDatabase.getCurrentUser()
            val isAdmin = current?.isAdmin == true
            navView?.menu?.findItem(R.id.drawer_admin)?.isVisible = isAdmin
        } catch (_: Exception) {}
        navView?.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.drawer_home -> { drawer?.closeDrawers(); true }
                R.id.drawer_events -> { navigateToEvents(); true }
                R.id.drawer_gallery -> { navigateToGallery(); true }
                R.id.drawer_documents -> { navigateToDocuments(); true }
                R.id.drawer_donate -> { navigateToDonate(); true }
                R.id.drawer_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); finish(); true }
                R.id.drawer_admin -> { startActivity(Intent(this, AdminActivity::class.java)); true }
                else -> false
            }
        }

        // External website CTA
        findViewById<android.widget.Button>(R.id.btnVisitWebsite)?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http://the-umbilo-temple.infinityfreeapp.com"))
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Unable to open website", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set click listeners for cards
        cardEvents.setOnClickListener {
            navigateToEvents()
        }
        
        cardDonate.setOnClickListener {
            navigateToDonate()
        }
        
        cardGallery.setOnClickListener {
            navigateToGallery()
        }
        
        cardDocuments.setOnClickListener {
            navigateToDocuments()
        }
        
        cardLogout.setOnClickListener {
            confirmLogout()
        }
        
        // Admin card disabled on Home

        // Load dynamic previews
        loadLatestGalleryPreviews()
        loadUpcomingEventPreview()
        
        // Ensure admin card stays hidden
    }

    private fun loadLatestGalleryPreviews() {
        val rv = findViewById<RecyclerView>(R.id.rvLatestGallery)
        latestRv = rv
        latestLm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv?.layoutManager = latestLm
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rv)
        findViewById<android.widget.Button>(R.id.btnSeeGallery)?.setOnClickListener {
            navigateToGallery()
        }
        documentRepository.getApprovedGalleryMedia(
            onSuccess = { docs ->
                val latest = docs.sortedByDescending { it.uploadDate }.take(3)
                rv?.adapter = LatestMediaAdapter(latest)
                if (latest.size > 1) {
                    startLatestCarousel()
                }
            },
            onError = { _ ->
                rv?.adapter = LatestMediaAdapter(emptyList())
                stopLatestCarousel()
            }
        )
    }

    private fun loadUpcomingEventPreview() {
        val tvName = findViewById<TextView>(R.id.tvUpcomingEventName)
        val tvDate = findViewById<TextView>(R.id.tvUpcomingEventDate)
        findViewById<android.widget.Button>(R.id.btnUpcomingEvents)?.setOnClickListener {
            navigateToEvents()
        }
        eventRepository.fetchEventsFromFirestore(
            onSuccess = { list ->
                val now = java.util.Date()
                val upcoming = list
                    .filter { it.date.after(now) || it.date == now }
                    .minByOrNull { it.date }
                    ?: list.maxByOrNull { it.date } // fallback to most recent if none upcoming
                if (upcoming != null) {
                    tvName?.text = upcoming.title
                    val df = java.text.SimpleDateFormat("dd MMM yyyy • h:mm a", java.util.Locale.getDefault())
                    tvDate?.text = df.format(upcoming.date)
                }
            },
            onError = { _ ->
                // leave defaults
            }
        )
    }

    private class LatestMediaAdapter(private val items: List<com.example.umbilotemplefrontend.models.Document>) : RecyclerView.Adapter<LatestMediaAdapter.VH>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val imageView = android.widget.ImageView(parent.context)
            val params = RecyclerView.LayoutParams(parent.resources.displayMetrics.density.let { (180 * it).toInt() }, parent.resources.displayMetrics.density.let { (120 * it).toInt() })
            params.leftMargin = (6 * parent.resources.displayMetrics.density).toInt()
            params.rightMargin = (6 * parent.resources.displayMetrics.density).toInt()
            imageView.layoutParams = params
            imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            return VH(imageView)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val doc = items.getOrNull(position)
            val iv = holder.imageView
            if (doc == null) {
                iv.setImageResource(R.drawable.ic_gallery)
                return
            }
            if (doc.fileType == "image") {
                Glide.with(iv.context)
                    .load(doc.filePath)
                    .thumbnail(0.25f)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .timeout(10000)
                    .centerCrop()
                    .into(iv)
            } else if (doc.fileType == "video") {
                Glide.with(iv.context)
                    .asBitmap()
                    .load(doc.filePath)
                    .apply(RequestOptions().frame(1_000_000))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .timeout(10000)
                    .centerCrop()
                    .into(iv)
            } else {
                iv.setImageResource(R.drawable.ic_documents)
            }
        }

        override fun getItemCount(): Int = maxOf(1, items.size)

        class VH(val imageView: android.widget.ImageView) : RecyclerView.ViewHolder(imageView)
    }

    private fun startLatestCarousel() {
        if (latestRv == null || latestLm == null) return
        if (latestCarouselHandler == null) {
            latestCarouselHandler = Handler(Looper.getMainLooper())
        }
        val handler = latestCarouselHandler ?: return
        latestCarouselRunnable?.let { handler.removeCallbacks(it) }
        latestCarouselRunnable = Runnable {
            val lm = latestLm ?: return@Runnable
            val rv = latestRv ?: return@Runnable
            val itemCount = rv.adapter?.itemCount ?: 0
            if (itemCount <= 1) return@Runnable
            val current = lm.findFirstVisibleItemPosition().coerceAtLeast(0)
            val next = (current + 1) % itemCount
            rv.smoothScrollToPosition(next)
            handler.postDelayed(latestCarouselRunnable!!, 3500)
        }
        handler.postDelayed(latestCarouselRunnable!!, 3500)
    }

    private fun stopLatestCarousel() {
        latestCarouselRunnable?.let { latestCarouselHandler?.removeCallbacks(it) }
        latestCarouselRunnable = null
    }

    override fun onPause() {
        super.onPause()
        stopLatestCarousel()
    }

    
    
    private fun navigateToEvents() {
        val intent = Intent(this, EventsActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    
    private fun navigateToDonate() {
        val intent = Intent(this, DonationsActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun logout() {
        // Clear current user session
        localDatabase.clearCurrentUser()
        
        // Navigate back to login screen
        navigateToLogin()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun navigateToAdmin() {
        // Check if user is admin
        val currentUser = localDatabase.getCurrentUser()
        Log.d("HomeActivity", "Current user: ${currentUser?.name}, isAdmin: ${currentUser?.isAdmin}")
        
        if (currentUser?.isAdmin == true) {
            try {
                // Make sure the user is marked as admin
                if (!currentUser.isAdmin) {
                    val updatedUser = currentUser.copy(isAdmin = true)
                    localDatabase.updateUser(updatedUser)
                    localDatabase.setCurrentUser(updatedUser)
                    Log.d("HomeActivity", "Updated user to admin status")
                }
                
                // Open admin activity
                val intent = Intent(this, AdminActivity::class.java)
                Log.d("HomeActivity", "Starting AdminActivity")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error opening AdminActivity", e)
                Toast.makeText(this, "Error opening admin panel: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "You don't have admin privileges", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToGallery() {
        val intent = Intent(this, GalleryActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToDocuments() {
        val intent = Intent(this, DocumentsActivity::class.java)
        startActivity(intent)
        finish()
    }
} 