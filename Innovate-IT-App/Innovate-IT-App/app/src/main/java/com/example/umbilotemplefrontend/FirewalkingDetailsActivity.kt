package com.example.umbilotemplefrontend

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.adapters.DocumentsAdapter
import com.example.umbilotemplefrontend.models.Document
import com.example.umbilotemplefrontend.models.Event
import com.example.umbilotemplefrontend.utils.DocumentRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.NavigationHandler
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirewalkingDetailsActivity : AppCompatActivity() {
    
    private lateinit var localDatabase: LocalDatabase
    private lateinit var documentRepository: DocumentRepository
    private lateinit var documentsAdapter: DocumentsAdapter
    private lateinit var navigationHandler: NavigationHandler
    private var eventId: String? = null
    private var event: Event? = null
    private val documents = mutableListOf<Document>()
    
    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firewalking_details)
        
        // Initialize local database and repository
        localDatabase = LocalDatabase(this)
        documentRepository = DocumentRepository(this)
        
        // Initialize navigation handler
        navigationHandler = NavigationHandler(this)
        
        // Check if user is logged in
        if (!navigationHandler.checkUserLoggedIn()) {
            return
        }
        
        // Get event ID from intent
        eventId = intent.getStringExtra("EVENT_ID")
        if (eventId == null) {
            Toast.makeText(this, "Error: Event not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Load event details
        loadEventDetails()
        
        // Set up UI components
        setupUI()
        
        // Set up navigation
        navigationHandler.setupNavigation()
    }
    
    private fun loadEventDetails() {
        val events = localDatabase.getEvents()
        event = events.find { it.id == eventId }
        
        if (event == null) {
            Toast.makeText(this, "Error: Event not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Load documents for this event from Firebase
        documentRepository.fetchDocumentsFromFirestore(
            onSuccess = { allDocuments ->
                documents.clear()
                documents.addAll(allDocuments.filter { it.eventId == eventId })
                documentsAdapter.notifyDataSetChanged()
            },
            onError = { exception ->
                // Use cached documents
                val allDocuments = documentRepository.getDocumentsFromCache()
                documents.clear()
                documents.addAll(allDocuments.filter { it.eventId == eventId })
                documentsAdapter.notifyDataSetChanged()
            }
        )
    }
    
    private fun setupUI() {
        // Set event details
        val tvFirewalkingTitle = findViewById<TextView>(R.id.tvFirewalkingTitle)
        val tvFirewalkingDate = findViewById<TextView>(R.id.tvFirewalkingDate)
        val tvFirewalkingTime = findViewById<TextView>(R.id.tvFirewalkingTime)
        
        event?.let {
            tvFirewalkingTitle.text = it.title
            tvFirewalkingDate.text = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(it.date)
            tvFirewalkingTime.text = it.time
        }
        
        // Set up documents RecyclerView
        val documentsRecyclerView = findViewById<RecyclerView>(R.id.documentsRecyclerView)
        documentsAdapter = DocumentsAdapter(this, documents)
        documentsRecyclerView.layoutManager = LinearLayoutManager(this)
        documentsRecyclerView.adapter = documentsAdapter
        
        // Set up upload button
        val btnUploadDocument = findViewById<Button>(R.id.btnUploadDocument)
        btnUploadDocument.setOnClickListener {
            openFilePicker()
        }
    }
    
    private fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }
    
    private fun handleSelectedFile(uri: Uri) {
        try {
            val fileName = getFileName(uri) ?: "Unknown file"
            val fileType = getFileExtension(fileName)
            val currentUser = localDatabase.getCurrentUser()
            val uploadedBy = currentUser?.name ?: "Unknown User"
            
            // Show progress
            Toast.makeText(this, "Uploading document...", Toast.LENGTH_SHORT).show()
            
            // Upload to Firebase Storage
            documentRepository.uploadDocument(
                fileUri = uri,
                fileName = fileName,
                fileType = fileType,
                uploadedBy = uploadedBy,
                eventId = eventId,
                isPublic = false,
                onProgress = { progress ->
                    // Optionally show upload progress
                },
                onSuccess = { document ->
                    // Check if activity is still valid before showing UI
                    if (!isFinishing && !isDestroyed) {
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                // Update UI
                                documents.add(document)
                                documentsAdapter.notifyItemInserted(documents.size - 1)
                                Toast.makeText(this@FirewalkingDetailsActivity, "Document uploaded successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onError = { exception ->
                    if (!isFinishing && !isDestroyed) {
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                Toast.makeText(this@FirewalkingDetailsActivity, "Error uploading document: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            if (!isFinishing && !isDestroyed) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this@FirewalkingDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex("_display_name")
                if (nameIndex >= 0) {
                    result = it.getString(nameIndex)
                }
            }
        }
        return result
    }
    
    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "")
    }
} 