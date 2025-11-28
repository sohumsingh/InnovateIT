package com.example.umbilotemplefrontend.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.Document
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentsAdapter(
    private val context: Context,
    private var documents: List<Document>
) : RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class DocumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDocumentName: TextView = view.findViewById(R.id.tvDocumentName)
        val tvDocumentDate: TextView = view.findViewById(R.id.tvDocumentDate)
        val tvDocumentType: TextView = view.findViewById(R.id.tvDocumentType)
        val ivDocumentIcon: ImageView = view.findViewById(R.id.ivDocumentIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun getItemCount() = documents.size

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documents[position]
        
        holder.tvDocumentName.text = document.name
        holder.tvDocumentDate.text = dateFormat.format(document.uploadDate)
        holder.tvDocumentType.text = document.fileType.uppercase()
        
        // Set icon based on file type
        val iconResId = when (document.fileType.lowercase()) {
            "pdf" -> R.drawable.ic_events // Using events icon for PDF
            "doc", "docx" -> R.drawable.ic_events
            "jpg", "jpeg", "png" -> R.drawable.ic_events // Using events icon for images
            else -> R.drawable.ic_events
        }
        holder.ivDocumentIcon.setImageResource(iconResId)
        
        // Set click listener to open the document
        holder.itemView.setOnClickListener {
            openDocument(document)
        }
    }
    
    private fun openDocument(document: Document) {
        try {
            val file = File(document.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
                return
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = getMimeType(document.fileType)
            intent.setDataAndType(uri, mimeType)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }
    
    fun updateDocuments(newDocuments: List<Document>) {
        this.documents = newDocuments
        notifyDataSetChanged()
    }
} 