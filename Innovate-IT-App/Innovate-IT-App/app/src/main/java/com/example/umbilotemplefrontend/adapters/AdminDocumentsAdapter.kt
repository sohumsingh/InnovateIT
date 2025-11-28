package com.example.umbilotemplefrontend.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.Document
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying documents in the admin panel with delete action
 */
class AdminDocumentsAdapter(
    private val context: Context,
    private var documents: List<Document>,
    private val listener: DocumentActionListener
) : RecyclerView.Adapter<AdminDocumentsAdapter.DocumentViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    interface DocumentActionListener {
        fun onDeleteDocument(document: Document, position: Int)
        fun onViewDocument(document: Document, position: Int)
    }

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDocumentName: TextView = itemView.findViewById(R.id.tvDocumentName)
        val tvDocumentTag: TextView = itemView.findViewById(R.id.tvDocumentTag)
        val tvDocumentInfo: TextView = itemView.findViewById(R.id.tvDocumentInfo)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteDocument)
        val btnView: ImageButton = itemView.findViewById(R.id.btnViewDocument)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_admin_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documents[position]

        holder.tvDocumentName.text = document.name
        val tagText = if (document.documentType.name == "GALLERY_MEDIA") "Gallery Upload" else "Personal Document"
        holder.tvDocumentTag.text = tagText
        holder.tvDocumentInfo.text = "Uploaded by: ${document.uploadedBy} on ${dateFormat.format(document.uploadDate)}"

        holder.btnDelete.setOnClickListener {
            listener.onDeleteDocument(document, position)
        }

        holder.btnView.setOnClickListener {
            listener.onViewDocument(document, position)
        }
    }

    override fun getItemCount(): Int = documents.size

    fun updateDocuments(newDocuments: List<Document>) {
        documents = newDocuments
        notifyDataSetChanged()
    }

    fun getDocuments(): List<Document> = documents
}

