package com.example.umbilotemplefrontend

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.umbilotemplefrontend.models.Document
import com.example.umbilotemplefrontend.models.MediaStatus
import com.example.umbilotemplefrontend.utils.DocumentRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.NavigationHandler
import java.text.SimpleDateFormat
import java.util.Locale

class GalleryActivity : AppCompatActivity() {
    
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var localDatabase: LocalDatabase
    private lateinit var documentRepository: DocumentRepository
    private lateinit var recyclerViewGallery: RecyclerView
    private lateinit var tvNoMedia: TextView
    private lateinit var galleryAdapter: GalleryAdapter
    private val allMedia = mutableListOf<Document>()
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_gallery)
            
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
        recyclerViewGallery = findViewById(R.id.recyclerViewGallery)
        tvNoMedia = findViewById(R.id.tvNoMedia)
        // Header title
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "GALLERY"
        
        // Set up back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        // Set up RecyclerView
        recyclerViewGallery.layoutManager = GridLayoutManager(this, 2)
        recyclerViewGallery.setHasFixedSize(true)
        recyclerViewGallery.itemAnimator = null
        galleryAdapter = GalleryAdapter { document -> showMediaDetails(document) }
        recyclerViewGallery.adapter = galleryAdapter
        
        // Load gallery media
        loadGalleryMedia()

        // Search filtering
        val etSearch = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchGallery)
        etSearch?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyGalleryFilter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        } catch (e: Exception) {
            Log.e("GalleryActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh gallery when returning to this screen
        loadGalleryMedia()
    }

    override fun onStart() {
        super.onStart()
        // Real-time updates: listen to all docs and filter to approved gallery
        try {
            documentRepository.startRealtimeListener(
                onDocumentsChanged = { docs ->
                    val approved = docs.filter { it.documentType.name == "GALLERY_MEDIA" && it.mediaStatus.name == "APPROVED" }
                    allMedia.clear()
                    allMedia.addAll(approved.sortedByDescending { it.uploadDate })
                    applyGalleryFilter(findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchGallery)?.text?.toString() ?: "")
                },
                onError = { _ -> }
            )
        } catch (_: Exception) { }
    }

    override fun onStop() {
        super.onStop()
        try { documentRepository.stopRealtimeListener() } catch (_: Exception) {}
    }
    
    private fun loadGalleryMedia() {
        documentRepository.getApprovedGalleryMedia(
            onSuccess = { documents ->
                allMedia.clear()
                allMedia.addAll(documents.sortedByDescending { it.uploadDate })
                applyGalleryFilter(findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchGallery)?.text?.toString() ?: "")
            },
            onError = { exception ->
                Toast.makeText(
                    this,
                    "Error loading gallery: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                allMedia.clear()
                applyGalleryFilter(findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchGallery)?.text?.toString() ?: "")
            }
        )
    }
    
    private fun displayMedia(documents: List<Document>) {
        if (documents.isEmpty()) {
            tvNoMedia.visibility = View.VISIBLE
            recyclerViewGallery.visibility = View.GONE
            return
        }
        
        tvNoMedia.visibility = View.GONE
        recyclerViewGallery.visibility = View.VISIBLE
        
        galleryAdapter.updateMedia(documents)
    }

    private fun applyGalleryFilter(query: String) {
        val filtered = if (query.isBlank()) {
            allMedia
        } else {
            val q = query.trim().lowercase(Locale.getDefault())
            allMedia.filter { doc ->
                val name = doc.name.lowercase(Locale.getDefault())
                val desc = doc.description.lowercase(Locale.getDefault())
                val uploader = doc.uploadedBy.lowercase(Locale.getDefault())
                val uploaderEmail = doc.uploadedByEmail.lowercase(Locale.getDefault())
                val fileType = doc.fileType.lowercase(Locale.getDefault())
                val dateStr = try { dateFormat.format(doc.uploadDate).lowercase(Locale.getDefault()) } catch (_: Exception) { "" }

                name.contains(q) ||
                desc.contains(q) ||
                uploader.contains(q) ||
                uploaderEmail.contains(q) ||
                fileType.contains(q) ||
                dateStr.contains(q)
            }
        }
        displayMedia(filtered)
    }
    
    private fun showMediaDetails(document: Document) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(document.name)
            .setMessage("""
                Description: ${document.description}
                
                Uploaded by: ${document.uploadedBy}
                Date: ${dateFormat.format(document.uploadDate)}
                
                File Type: ${document.fileType}
            """.trimIndent())
            .setPositiveButton("View Full Screen") { _, _ ->
                // Open full screen image viewer or video player
                openMediaFullScreen(document)
            }
            .setNeutralButton("Share") { _, _ ->
                shareMedia(document)
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun openMediaFullScreen(document: Document) {
        try {
            if (document.fileType == "image") {
                val intent = Intent(this, FullscreenImageActivity::class.java)
                intent.putExtra("IMAGE_URL", document.filePath)
                startActivity(intent)
            } else {
                val uri = android.net.Uri.parse(document.filePath)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, if (document.fileType == "video") "video/*" else "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(viewIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open media", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareMedia(document: Document) {
        try {
            val shareText = buildString {
                append("Check this out from Umbilo Temple gallery: ")
                if (!document.description.isNullOrEmpty()) {
                    append("\n\n")
                    append(document.description)
                }
                if (!document.filePath.isNullOrEmpty()) {
                    append("\n\n")
                    append(document.filePath)
                }
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, document.name)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share media"))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to share media", Toast.LENGTH_SHORT).show()
        }
    }
    
    private class GalleryAdapter(
        private val onMediaClick: (Document) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.MediaViewHolder>() {
        
        private val mediaList = mutableListOf<Document>()
        
        fun updateMedia(documents: List<Document>) {
            mediaList.clear()
            mediaList.addAll(documents)
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery_media, parent, false)
            return MediaViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            holder.bind(mediaList[position], onMediaClick)
        }
        
        override fun getItemCount(): Int = mediaList.size
        
        class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivPreview: ImageView = itemView.findViewById(R.id.ivGalleryPreview)
            private val tvDescription: TextView = itemView.findViewById(R.id.tvGalleryDescription)
            
            fun bind(document: Document, onMediaClick: (Document) -> Unit) {
                tvDescription.text = document.description.ifEmpty { document.name }
                
                // Load preview using Glide (image or video frame)
                if (document.fileType == "image") {
                    Glide.with(itemView.context)
                        .load(document.filePath)
                        .thumbnail(0.25f)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .timeout(10000)
                        .placeholder(R.drawable.ic_events)
                        .error(R.drawable.ic_events)
                        .centerCrop()
                        .into(ivPreview)
                } else {
                    when (document.fileType) {
                        "video" -> {
                            Glide.with(itemView.context)
                                .asBitmap()
                                .load(document.filePath)
                                .apply(RequestOptions().frame(1_000_000))
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                .timeout(10000)
                                .placeholder(R.drawable.ic_events)
                                .error(R.drawable.ic_events)
                                .centerCrop()
                                .into(ivPreview)
                        }
                        else -> {
                            ivPreview.setImageResource(R.drawable.ic_documents)
                        }
                    }
                }
                
                itemView.setOnClickListener {
                    onMediaClick(document)
                }
            }
        }
    }
}

