package com.example.umbilotemplefrontend

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.umbilotemplefrontend.models.DocumentType
import com.example.umbilotemplefrontend.models.MediaStatus
import com.example.umbilotemplefrontend.utils.DocumentRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.NavigationHandler
import java.util.Date
import java.util.UUID

class DocumentsActivity : AppCompatActivity() {
    
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var localDatabase: LocalDatabase
    private lateinit var documentRepository: DocumentRepository
    private lateinit var btnUploadDocument: Button
    private lateinit var btnUploadGallery: Button
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, can now pick file
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFileSelected(uri)
            }
        }
    }
    
    private var currentDocumentType: DocumentType? = null
    private var selectedFileUri: Uri? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_documents)
            
            // Initialize database and repository
            localDatabase = LocalDatabase(this)
            documentRepository = DocumentRepository(this)
        
        // Initialize navigation handler
        navigationHandler = NavigationHandler(this)
        
        // Check if user is logged in
        if (!navigationHandler.checkUserLoggedIn()) {
            return
        }
        
        // Set up navigation
        navigationHandler.setupNavigation()
        
        // Initialize views
        btnUploadDocument = findViewById(R.id.btnUploadDocument)
        btnUploadGallery = findViewById(R.id.btnUploadGallery)
        // Header title
        findViewById<android.widget.TextView>(R.id.tvHeaderTitle)?.text = "DOCUMENTS"
        
        // Set up back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        // Set up document upload button
        btnUploadDocument.setOnClickListener {
            Log.d("DocumentsActivity", "Upload Document button clicked")
            currentDocumentType = DocumentType.DOCUMENT
            pickFile()
        }
        
        // Set up gallery upload button
        btnUploadGallery.setOnClickListener {
            Log.d("DocumentsActivity", "Upload Gallery button clicked")
            currentDocumentType = DocumentType.GALLERY_MEDIA
            pickFile()
        }
        
        } catch (e: Exception) {
            Log.e("DocumentsActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error opening documents: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun pickFile() {
        Log.d("DocumentsActivity", "pickFile called, documentType: $currentDocumentType")
        // ACTION_GET_CONTENT doesn't require READ_EXTERNAL_STORAGE permission on API 19+
        startFilePicker()
    }
    
    private fun startFilePicker() {
        Log.d("DocumentsActivity", "startFilePicker called")
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                        "image/*",
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    ))
                }
            }
            val chooserIntent = Intent.createChooser(intent, "Select File")
            pickFileLauncher.launch(chooserIntent)
            Log.d("DocumentsActivity", "File picker launched")
        } catch (e: Exception) {
            Log.e("DocumentsActivity", "Error launching file picker", e)
            Toast.makeText(this, "Error opening file picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleFileSelected(uri: Uri) {
        Log.d("DocumentsActivity", "handleFileSelected: $uri")
        selectedFileUri = uri
        
        when (currentDocumentType) {
            DocumentType.DOCUMENT -> {
                // For documents, just ask for optional description
                Log.d("DocumentsActivity", "Showing document upload dialog")
                showDocumentUploadDialog(null)
            }
            DocumentType.GALLERY_MEDIA -> {
                // For gallery, require description
                Log.d("DocumentsActivity", "Showing gallery upload dialog")
                showGalleryUploadDialog()
            }
            null -> {
                Log.e("DocumentsActivity", "currentDocumentType is null")
            }
        }
    }
    
    private fun showDocumentUploadDialog(description: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_document_upload, null)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDocumentDescription)
        val tilDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDocumentDescription)
        
        tilDescription.hint = "Description (Optional)"
        etDescription.setText(description ?: "")
        
        AlertDialog.Builder(this)
            .setTitle("Upload Document")
            .setView(dialogView)
            .setPositiveButton("Upload") { _, _ ->
                val currentUser = localDatabase.getCurrentUser()
                if (currentUser == null || selectedFileUri == null || currentDocumentType == null) {
                    Toast.makeText(this, "Error: Missing information", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val description = etDescription.text.toString().trim()
                uploadFile(currentUser, description, currentDocumentType!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showGalleryUploadDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_document_upload, null)
        val tilDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDocumentDescription)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDocumentDescription)
        tilDescription.hint = "Description *"
        
        AlertDialog.Builder(this)
            .setTitle("Upload to Gallery")
            .setMessage("Provide a description for your media. This will be reviewed by admin before being visible to all users.")
            .setView(dialogView)
            .setPositiveButton("Upload") { _, _ ->
                val currentUser = localDatabase.getCurrentUser()
                if (currentUser == null || selectedFileUri == null || currentDocumentType == null) {
                    Toast.makeText(this, "Error: Missing information", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val description = etDescription.text.toString().trim()
                if (description.isEmpty()) {
                    tilDescription.error = "Description is required"
                    return@setPositiveButton
                } else {
                    tilDescription.error = null
                }
                
                uploadFile(currentUser, description, currentDocumentType!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun uploadFile(user: com.example.umbilotemplefrontend.models.User, description: String, documentType: DocumentType) {
        Log.d("DocumentsActivity", "uploadFile called for user: ${user.name}, type: $documentType")
        selectedFileUri?.let { uri ->
            val fileName = getFileName(uri) ?: "document_${UUID.randomUUID()}"
            val fileExtension = fileName.substringAfterLast('.', "")
            val fileType = getFileType(fileExtension)
            
            Log.d("DocumentsActivity", "Starting upload: fileName=$fileName, fileType=$fileType")
            
            documentRepository.uploadDocumentFile(
                fileUri = uri,
                fileName = fileName,
                fileType = fileType,
                uploadedBy = user.name,
                uploadedByEmail = user.email,
                documentType = documentType,
                description = description,
                onProgress = { progress ->
                    // Could show progress dialog here
                },
                onSuccess = { document ->
                    // Check if activity is still valid before showing UI
                    if (!isFinishing && !isDestroyed) {
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                Toast.makeText(this@DocumentsActivity, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                                
                                // Reset state
                                selectedFileUri = null
                                currentDocumentType = null
                                
                                // Show success message
                                val message = when (documentType) {
                                    DocumentType.DOCUMENT -> "Your document has been submitted for admin review."
                                    DocumentType.GALLERY_MEDIA -> "Your media has been submitted and will be reviewed by admin. It will appear in gallery once approved."
                                }
                                
                                AlertDialog.Builder(this@DocumentsActivity)
                                    .setTitle("Upload Successful")
                                    .setMessage(message)
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                        }
                    }
                },
                onError = { exception ->
                    if (!isFinishing && !isDestroyed) {
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                Toast.makeText(this@DocumentsActivity, "Error uploading file: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
    
    private fun getFileType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp" -> "image"
            "pdf" -> "pdf"
            "doc", "docx" -> "document"
            "mp4", "avi", "mov" -> "video"
            else -> "other"
        }
    }
}

