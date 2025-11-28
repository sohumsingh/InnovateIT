package com.example.umbilotemplefrontend.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.Document
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying gallery media in admin panel for approval
 */
class AdminGalleryAdapter(
    private val context: android.content.Context,
    private var mediaList: List<Document>,
    private val listener: GalleryActionListener
) : RecyclerView.Adapter<AdminGalleryAdapter.MediaViewHolder>() {
    
    interface GalleryActionListener {
        fun onApproveMedia(document: Document, position: Int)
        fun onRejectMedia(document: Document, position: Int)
        fun onViewMediaDetails(document: Document, position: Int)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_admin_gallery, parent, false)
        return MediaViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaList[position], listener)
    }
    
    override fun getItemCount(): Int = mediaList.size
    
    fun updateMedia(newMediaList: List<Document>) {
        mediaList = newMediaList
        notifyDataSetChanged()
    }
    
    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPreview: ImageView = itemView.findViewById(R.id.ivMediaPreview)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvMediaTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvUploadedBy: TextView = itemView.findViewById(R.id.tvUploadedBy)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
        
        fun bind(document: Document, listener: GalleryActionListener) {
            tvTitle.text = document.name
            tvDescription.text = document.description.ifEmpty { "No description" }
            tvUploadedBy.text = "Uploaded by: ${document.uploadedBy}"
            tvDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(document.uploadDate)
            
            // Load image using Glide
            if (document.fileType == "image") {
                Glide.with(itemView.context)
                    .load(document.filePath)
                    .placeholder(R.drawable.ic_events)
                    .error(R.drawable.ic_events)
                    .centerCrop()
                    .into(ivPreview)
            } else {
                when (document.fileType) {
                    "video" -> ivPreview.setImageResource(R.drawable.ic_events) // TODO: Add video icon
                    "pdf" -> ivPreview.setImageResource(R.drawable.ic_documents)
                    else -> ivPreview.setImageResource(R.drawable.ic_events)
                }
            }
            
            btnApprove.setOnClickListener {
                listener.onApproveMedia(document, adapterPosition)
            }
            
            btnReject.setOnClickListener {
                listener.onRejectMedia(document, adapterPosition)
            }
            
            btnViewDetails.setOnClickListener {
                listener.onViewMediaDetails(document, adapterPosition)
            }
        }
    }
}

