package com.example.umbilotemplefrontend.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.umbilotemplefrontend.models.Document
import com.example.umbilotemplefrontend.models.DocumentType
import com.example.umbilotemplefrontend.models.MediaStatus

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * Repository for managing Document data with Firebase Storage and Firestore sync
 */
class DocumentRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val documentsCollection = db.collection("documents")
    private val storageRef = storage.reference.child("documents")
    private val localDatabase: LocalDatabase by lazy { LocalDatabase(context) }
    private var documentsListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "DocumentRepository"
    }

    /**
     * Fetch all documents from Firestore
     */
    fun fetchDocumentsFromFirestore(
        onSuccess: (List<Document>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        documentsCollection.get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents.mapNotNull { doc ->
                    try {
                        documentToDocument(doc.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document: ${doc.id}", e)
                        null
                    }
                }
                // Cache locally
                try {
                    saveDocumentsToCache(documents)
                } catch (e: Exception) {
                    Log.e(TAG, "Error caching documents", e)
                }
                onSuccess(documents)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching documents from Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Upload document file to Firebase Storage and save metadata to Firestore
     */
    fun uploadDocument(
        fileUri: Uri,
        fileName: String,
        fileType: String,
        uploadedBy: String,
        eventId: String? = null,
        isPublic: Boolean = false,
        onProgress: (Int) -> Unit,
        onSuccess: (Document) -> Unit,
        onError: (Exception) -> Unit
    ) {
        uploadDocumentFile(
            fileUri = fileUri,
            fileName = fileName,
            fileType = fileType,
            uploadedBy = uploadedBy,
            uploadedByEmail = "",
            documentType = DocumentType.DOCUMENT,
            description = "",
            eventId = eventId,
            isPublic = isPublic,
            onProgress = onProgress,
            onSuccess = onSuccess,
            onError = onError
        )
    }
    
    /**
     * Upload document file to Firebase Storage and save metadata to Firestore
     * Updated version with new Document model fields
     */
    fun uploadDocumentFile(
        fileUri: Uri,
        fileName: String,
        fileType: String,
        uploadedBy: String,
        uploadedByEmail: String = "",
        documentType: DocumentType = DocumentType.DOCUMENT,
        description: String = "",
        eventId: String? = null,
        isPublic: Boolean = false,
        onProgress: (Int) -> Unit,
        onSuccess: (Document) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Check Firebase Auth state
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        
        Log.d(TAG, "Uploading file: $fileName")
        Log.d(TAG, "Firebase Auth current user: ${currentUser?.uid}, isAnonymous: ${currentUser?.isAnonymous}")
        
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated with Firebase Auth - signing in anonymously")
            firebaseAuth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Anonymous sign-in successful, retrying upload")
                        performUpload(fileUri, fileName, fileType, uploadedBy, uploadedByEmail, documentType, description, eventId, isPublic, onProgress, onSuccess, onError)
                    } else {
                        Log.e(TAG, "Anonymous sign-in failed", task.exception)
                        onError(task.exception ?: Exception("Failed to authenticate"))
                    }
                }
        } else {
            performUpload(fileUri, fileName, fileType, uploadedBy, uploadedByEmail, documentType, description, eventId, isPublic, onProgress, onSuccess, onError)
        }
    }
    
    private fun performUpload(
        fileUri: Uri,
        fileName: String,
        fileType: String,
        uploadedBy: String,
        uploadedByEmail: String,
        documentType: DocumentType,
        description: String,
        eventId: String?,
        isPublic: Boolean,
        onProgress: (Int) -> Unit,
        onSuccess: (Document) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val documentId = UUID.randomUUID().toString()
        val fileRef = storageRef.child("$documentId/$fileName")

        // Upload file to Firebase Storage
        fileRef.putFile(fileUri)
            .addOnProgressListener { taskSnapshot: UploadTask.TaskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                onProgress(progress)
            }
            .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                // Get download URL
                fileRef.downloadUrl.addOnSuccessListener { downloadUri: Uri ->
                    // Create document metadata
                    val document = Document(
                        id = documentId,
                        name = fileName,
                        description = description,
                        filePath = downloadUri.toString(), // Store Firebase Storage URL
                        uploadDate = Date(),
                        uploadedBy = uploadedBy,
                        uploadedByEmail = uploadedByEmail,
                        fileType = fileType,
                        documentType = documentType,
                        mediaStatus = MediaStatus.PENDING,
                        eventId = eventId,
                        isPublic = isPublic
                    )

                    // Save metadata to Firestore
                    saveDocumentMetadataToFirestore(document, onSuccess, onError)
                }.addOnFailureListener { exception: Exception ->
                    Log.e(TAG, "Error getting download URL", exception)
                    onError(exception)
                }
            }
            .addOnFailureListener { exception: Exception ->
                Log.e(TAG, "Error uploading file", exception)
                onError(exception)
            }
    }

    /**
     * Save document metadata to Firestore
     */
    private fun saveDocumentMetadataToFirestore(
        document: Document,
        onSuccess: (Document) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val documentMap = documentToMap(document)
        documentsCollection.document(document.id).set(documentMap)
            .addOnSuccessListener {
                Log.d(TAG, "Document metadata saved to Firestore: ${document.id}")
                // Update local cache - TEMPORARILY DISABLED
                // val documents = localDatabase.getDocuments().toMutableList()
                // documents.add(document)
                // localDatabase.saveDocuments(documents)
                onSuccess(document)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error saving document metadata to Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Update document metadata in Firestore
     */
    fun updateDocumentInFirestore(
        document: Document,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val documentMap = documentToMap(document)
        documentsCollection.document(document.id).set(documentMap)
            .addOnSuccessListener {
                Log.d(TAG, "Document updated in Firestore: ${document.id}")
                // Update local cache - TEMPORARILY DISABLED
                // val documents = localDatabase.getDocuments().toMutableList()
                // val existingIndex = documents.indexOfFirst { it.id == document.id }
                // if (existingIndex >= 0) {
                //     documents[existingIndex] = document
                // } else {
                //     documents.add(document)
                // }
                // localDatabase.saveDocuments(documents)
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error updating document in Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Delete document from both Firebase Storage and Firestore
     */
    fun deleteDocumentFromFirebase(
        documentId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // First get the document to know the file path
        documentsCollection.document(documentId).get()
            .addOnSuccessListener { doc ->
                val document = documentToDocument(doc.data as? Map<String, Any>)
                
                if (document != null && document.filePath.startsWith("https://")) {
                    // Delete file from Storage
                    val fileRef = storage.getReferenceFromUrl(document.filePath)
                    fileRef.delete()
                        .addOnSuccessListener {
                            // Delete metadata from Firestore
                            deleteDocumentMetadata(documentId, onSuccess, onError)
                        }
                        .addOnFailureListener { exception: Exception ->
                            Log.e(TAG, "Error deleting file from storage, continuing to delete metadata", exception)
                            // Continue to delete metadata even if file deletion fails
                            deleteDocumentMetadata(documentId, onSuccess, onError)
                        }
                } else {
                    // Just delete metadata if no storage file
                    deleteDocumentMetadata(documentId, onSuccess, onError)
                }
            }
            .addOnFailureListener { exception: Exception ->
                Log.e(TAG, "Error getting document for deletion", exception)
                onError(exception)
            }
    }

    /**
     * Delete document metadata from Firestore
     */
    private fun deleteDocumentMetadata(
        documentId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        documentsCollection.document(documentId).delete()
            .addOnSuccessListener {
                Log.d(TAG, "Document deleted from Firestore: $documentId")
                // Update local cache - TEMPORARILY DISABLED
                // val documents = localDatabase.getDocuments().toMutableList()
                // val iterator = documents.iterator()
                // while (iterator.hasNext()) {
                //     if (iterator.next().id == documentId) {
                //         iterator.remove()
                //         break
                //     }
                // }
                // localDatabase.saveDocuments(documents)
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting document from Firestore", exception)
                onError(exception)
            }
    }

    /**
     * Get documents from local cache
     */
    fun getDocumentsFromCache(): List<Document> {
        return try {
            localDatabase.getDocuments()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting documents from cache", e)
            emptyList()
        }
    }

    /**
     * Get documents for a specific event
     */
    fun getDocumentsByEventId(eventId: String): List<Document> {
        return try {
            localDatabase.getDocuments().filter { it.eventId == eventId }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting documents by event ID", e)
            emptyList()
        }
    }

    /**
     * Save documents to local cache
     */
    fun saveDocumentsToCache(documents: List<Document>) {
        try {
            localDatabase.saveDocuments(documents)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving documents to cache", e)
            // Don't throw - silently fail to prevent app crash
        }
    }

    /**
     * Start real-time listener for documents
     */
    fun startRealtimeListener(
        onDocumentsChanged: (List<Document>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        documentsListener = documentsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to documents", error)
                onError(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val documents = snapshot.documents.mapNotNull { doc ->
                    try {
                        documentToDocument(doc.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document: ${doc.id}", e)
                        null
                    }
                }
                try {
                    saveDocumentsToCache(documents)
                } catch (e: Exception) {
                    Log.e(TAG, "Error caching documents in listener", e)
                }
                onDocumentsChanged(documents)
            }
        }
    }

    /**
     * Stop real-time listener
     */
    fun stopRealtimeListener() {
        documentsListener?.remove()
        documentsListener = null
    }
    
    /**
     * Get approved gallery media
     */
    fun getApprovedGalleryMedia(
        onSuccess: (List<Document>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        documentsCollection
            .whereEqualTo("documentType", DocumentType.GALLERY_MEDIA.name)
            .whereEqualTo("mediaStatus", MediaStatus.APPROVED.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents.mapNotNull { doc ->
                    try {
                        documentToDocument(doc.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document: ${doc.id}", e)
                        null
                    }
                }
                onSuccess(documents)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching approved gallery media", exception)
                onError(exception)
            }
    }
    
    /**
     * Get pending gallery media for admin approval
     */
    fun getPendingGalleryMedia(
        onSuccess: (List<Document>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        documentsCollection
            .whereEqualTo("documentType", DocumentType.GALLERY_MEDIA.name)
            .whereEqualTo("mediaStatus", MediaStatus.PENDING.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents.mapNotNull { doc ->
                    try {
                        documentToDocument(doc.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document: ${doc.id}", e)
                        null
                    }
                }
                onSuccess(documents)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching pending gallery media", exception)
                onError(exception)
            }
    }
    
    /**
     * Approve or reject gallery media
     */
    fun approveOrRejectMedia(
        documentId: String,
        status: MediaStatus,
        adminMessage: String = "",
        reviewedBy: String? = null,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "mediaStatus" to status.name,
            "adminMessage" to adminMessage,
            "reviewedDate" to Date()
        )
        
        reviewedBy?.let { updates["reviewedBy"] = it }
        
        documentsCollection.document(documentId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Media ${status.name.lowercase()}: $documentId")
                // Update local cache - TEMPORARILY DISABLED
                // val documents = localDatabase.getDocuments().toMutableList()
                // val existingIndex = documents.indexOfFirst { it.id == documentId }
                // if (existingIndex >= 0) {
                //     val updatedDoc = documents[existingIndex].copy(
                //         mediaStatus = status,
                //         adminMessage = adminMessage,
                //         reviewedBy = reviewedBy,
                //         reviewedDate = Date()
                //     )
                //     documents[existingIndex] = updatedDoc
                //     localDatabase.saveDocuments(documents)
                // }
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error updating media status", exception)
                onError(exception)
            }
    }

    /**
     * Convert Document to Map for Firestore
     */
    private fun documentToMap(document: Document): Map<String, Any?> {
        return mapOf(
            "id" to document.id,
            "name" to document.name,
            "description" to document.description,
            "filePath" to document.filePath,
            "uploadDate" to document.uploadDate,
            "uploadedBy" to document.uploadedBy,
            "uploadedByEmail" to document.uploadedByEmail,
            "fileType" to document.fileType,
            "documentType" to document.documentType.name,
            "mediaStatus" to document.mediaStatus.name,
            "eventId" to document.eventId,
            "isPublic" to document.isPublic,
            "reviewedBy" to document.reviewedBy,
            "reviewedDate" to document.reviewedDate,
            "adminMessage" to document.adminMessage
        )
    }

    /**
     * Convert Firestore document to Document
     */
    private fun documentToDocument(data: Map<String, Any?>?): Document? {
        if (data == null) return null

        return try {
            // Get DocumentType enum with default fallback
            val docTypeStr = data["documentType"] as? String ?: "DOCUMENT"
            val documentType = try {
                DocumentType.valueOf(docTypeStr)
            } catch (e: Exception) {
                DocumentType.DOCUMENT
            }
            
            // Get MediaStatus enum with default fallback
            val statusStr = data["mediaStatus"] as? String ?: "PENDING"
            val mediaStatus = try {
                MediaStatus.valueOf(statusStr)
            } catch (e: Exception) {
                MediaStatus.PENDING
            }
            
            Document(
                id = data["id"] as? String ?: return null,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                filePath = data["filePath"] as? String ?: "",
                uploadDate = (data["uploadDate"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                uploadedBy = data["uploadedBy"] as? String ?: "",
                uploadedByEmail = data["uploadedByEmail"] as? String ?: "",
                fileType = data["fileType"] as? String ?: "",
                documentType = documentType,
                mediaStatus = mediaStatus,
                eventId = data["eventId"] as? String?,
                isPublic = data["isPublic"] as? Boolean ?: false,
                reviewedBy = data["reviewedBy"] as? String?,
                reviewedDate = (data["reviewedDate"] as? com.google.firebase.Timestamp)?.toDate(),
                adminMessage = data["adminMessage"] as? String ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to Document", e)
            null
        }
    }
}

