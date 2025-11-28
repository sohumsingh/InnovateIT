package com.example.umbilotemplefrontend.models

import java.io.Serializable
import java.util.Date

/**
 * Enum class for Document types
 */
enum class DocumentType : Serializable {
    DOCUMENT,      // Regular documents for admin review
    GALLERY_MEDIA   // Gallery media for user viewing
}

/**
 * Enum class for Media approval status
 */
enum class MediaStatus : Serializable {
    PENDING,    // Waiting for admin approval
    APPROVED,   // Approved and visible to users
    REJECTED    // Rejected by admin
}

/**
 * Model class for Documents and Gallery Media
 */
data class Document(
    val id: String,
    val name: String,
    val description: String = "",  // Description for gallery media
    val filePath: String,
    val uploadDate: Date,
    val uploadedBy: String,
    val uploadedByEmail: String = "",  // Email of uploader
    val fileType: String,
    val documentType: DocumentType = DocumentType.DOCUMENT,
    val mediaStatus: MediaStatus = MediaStatus.PENDING,
    val eventId: String? = null,
    val isPublic: Boolean = false,
    val reviewedBy: String? = null,  // Admin who reviewed
    val reviewedDate: Date? = null,
    val adminMessage: String = ""  // Admin feedback for rejection
) : Serializable 