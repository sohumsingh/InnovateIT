package com.example.umbilotemplefrontend

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.umbilotemplefrontend.models.BookingStatus
import com.example.umbilotemplefrontend.utils.BookingRepository
import com.example.umbilotemplefrontend.utils.DocumentRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.SettingsManager
import com.example.umbilotemplefrontend.utils.NavigationHandler

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var localDatabase: LocalDatabase
    private lateinit var documentRepository: DocumentRepository
    private lateinit var bookingRepository: BookingRepository
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var switchTheme: Switch
    private lateinit var tvThemeLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply current theme as early as possible to avoid UI mismatch
        AppCompatDelegate.setDefaultNightMode(
            if (SettingsManager(this).isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        setContentView(R.layout.activity_settings)

        settingsManager = SettingsManager(this)
        localDatabase = LocalDatabase(this)
        documentRepository = DocumentRepository(this)
        bookingRepository = BookingRepository(this)

        // Init navigation and session check
        navigationHandler = NavigationHandler(this)
        if (!navigationHandler.checkUserLoggedIn()) {
            return
        }
        // Header title and back navigation, matching EventsActivity style
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "SETTINGS"
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = android.content.Intent(this, HomeActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Set up navigation after header is initialized
        navigationHandler.setupNavigation()

        switchTheme = findViewById(R.id.switchTheme)
        tvThemeLabel = findViewById(R.id.tvThemeLabel)
        switchTheme.isChecked = settingsManager.isDarkMode()
        tvThemeLabel.text = if (switchTheme.isChecked) "Dark Mode" else "Light Mode"
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setDarkMode(isChecked)
            tvThemeLabel.text = if (isChecked) "Dark Mode" else "Light Mode"
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            // Recreate to ensure views rebind with night qualifiers (buttons/text colors)
            recreate()
        }

        findViewById<Button>(R.id.btnUpdateAccount).setOnClickListener {
            showUpdateAccountDialog()
        }

        findViewById<Button>(R.id.btnMyUploads).setOnClickListener {
            val user = localDatabase.getCurrentUser() ?: return@setOnClickListener
            documentRepository.fetchDocumentsFromFirestore(
                onSuccess = { docs ->
                    val mine = docs.filter { it.uploadedByEmail == user.email }
                    val pending = mine.filter { it.mediaStatus.name != "APPROVED" }
                    showSimpleListDialog("My Uploads", mine.map { "${it.name} - ${it.mediaStatus.name}" })
                    if (pending.isNotEmpty()) {
                        Toast.makeText(this, "Pending: ${pending.size}", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { _ ->
                    Toast.makeText(this, "Unable to load uploads", Toast.LENGTH_SHORT).show()
                }
            )
        }

        findViewById<Button>(R.id.btnPendingEvents).setOnClickListener {
            val user = localDatabase.getCurrentUser() ?: return@setOnClickListener
            bookingRepository.fetchBookingsFromFirestore(
                onSuccess = { bookings ->
                    val mine = bookings.filter { it.userId == user.id && it.status == BookingStatus.PENDING }
                    showSimpleListDialog("Pending Event Requests", mine.map { "${it.eventName} - ${it.requestedDate}" })
                },
                onError = { _ ->
                    Toast.makeText(this, "Unable to load pending events", Toast.LENGTH_SHORT).show()
                }
            )
        }

        findViewById<Button>(R.id.btnHelpFaq).setOnClickListener {
            val intent = android.content.Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnLogoutSettings).setOnClickListener {
            try {
                localDatabase.clearCurrentUser()
                val intent = android.content.Intent(this, LoginActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure label always reflects the current persisted mode when returning
        if (::switchTheme.isInitialized && ::tvThemeLabel.isInitialized) {
            val isDark = settingsManager.isDarkMode()
            if (switchTheme.isChecked != isDark) {
                switchTheme.isChecked = isDark
            }
            tvThemeLabel.text = if (isDark) "Dark Mode" else "Light Mode"
        }
    }

    override fun onBackPressed() {
        val intent = android.content.Intent(this, HomeActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showUpdateAccountDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_account, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etPhone = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPhone)
        val user = localDatabase.getCurrentUser()
        etName.setText(user?.name ?: "")
        etPhone.setText(user?.phoneNumber ?: "")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Update Account")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updated = user?.copy(name = etName.text?.toString()?.trim() ?: "", phoneNumber = etPhone.text?.toString()?.trim() ?: "")
                if (updated != null) {
                    localDatabase.updateUser(updated)
                    localDatabase.setCurrentUser(updated)
                    Toast.makeText(this, "Account updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSimpleListDialog(title: String, items: List<String>) {
        val list = if (items.isEmpty()) listOf("No items found") else items
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(list.toTypedArray(), null)
            .setPositiveButton("OK", null)
            .show()
    }
}


