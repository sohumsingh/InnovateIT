package com.example.umbilotemplefrontend.utils

import android.content.Context
import android.util.Log
import com.example.umbilotemplefrontend.models.BankAccount
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Repository for managing bank accounts with Firebase Firestore sync
 * Handles both cloud storage and local caching for offline access
 */
class BankAccountRepository(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val localDatabase = LocalDatabase(context)
    private val collectionName = "bankAccounts"
    private var listenerRegistration: ListenerRegistration? = null
    
    companion object {
        private const val TAG = "BankAccountRepo"
    }
    
    /**
     * Fetch bank accounts from Firestore and cache locally
     */
    fun fetchBankAccountsFromFirestore(
        onSuccess: (List<BankAccount>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection(collectionName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val accounts = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        BankAccount(
                            id = doc.id,
                            bankName = doc.getString("bankName") ?: "",
                            accountNumber = doc.getString("accountNumber") ?: "",
                            branchCode = doc.getString("branchCode") ?: "",
                            iconResId = doc.getLong("iconResId")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing bank account: ${doc.id}", e)
                        null
                    }
                }
                
                // Cache locally
                localDatabase.saveBankAccounts(accounts)
                onSuccess(accounts)
                
                Log.d(TAG, "Fetched ${accounts.size} bank accounts from Firestore")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching bank accounts from Firestore", exception)
                // Return cached data on failure
                val cachedAccounts = localDatabase.getBankAccounts()
                if (cachedAccounts.isNotEmpty()) {
                    Log.d(TAG, "Using ${cachedAccounts.size} cached bank accounts")
                    onSuccess(cachedAccounts)
                } else {
                    onError(exception)
                }
            }
    }
    
    /**
     * Save a bank account to Firestore and local cache
     */
    fun saveBankAccountToFirestore(
        account: BankAccount,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val accountMap = hashMapOf(
            "bankName" to account.bankName,
            "accountNumber" to account.accountNumber,
            "branchCode" to account.branchCode,
            "iconResId" to account.iconResId
        )
        
        firestore.collection(collectionName)
            .document(account.id)
            .set(accountMap)
            .addOnSuccessListener {
                // Save to local cache
                localDatabase.addBankAccount(account)
                onSuccess()
                Log.d(TAG, "Bank account saved to Firestore: ${account.bankName}")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error saving bank account to Firestore", exception)
                // Still save locally for offline use
                localDatabase.addBankAccount(account)
                onError(exception)
            }
    }
    
    /**
     * Update a bank account in Firestore and local cache
     */
    fun updateBankAccountInFirestore(
        account: BankAccount,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val accountMap = hashMapOf(
            "bankName" to account.bankName,
            "accountNumber" to account.accountNumber,
            "branchCode" to account.branchCode,
            "iconResId" to account.iconResId
        )
        
        firestore.collection(collectionName)
            .document(account.id)
            .update(accountMap as Map<String, Any>)
            .addOnSuccessListener {
                // Update local cache
                localDatabase.updateBankAccount(account)
                onSuccess()
                Log.d(TAG, "Bank account updated in Firestore: ${account.bankName}")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error updating bank account in Firestore", exception)
                // Still update locally
                localDatabase.updateBankAccount(account)
                onError(exception)
            }
    }
    
    /**
     * Delete a bank account from Firestore and local cache
     */
    fun deleteBankAccountFromFirestore(
        accountId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection(collectionName)
            .document(accountId)
            .delete()
            .addOnSuccessListener {
                // Delete from local cache
                localDatabase.deleteBankAccount(accountId)
                onSuccess()
                Log.d(TAG, "Bank account deleted from Firestore: $accountId")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting bank account from Firestore", exception)
                // Still delete locally
                localDatabase.deleteBankAccount(accountId)
                onError(exception)
            }
    }
    
    /**
     * Set up real-time listener for bank accounts
     * Automatically updates local cache when cloud data changes
     */
    fun startRealtimeListener(onUpdate: (List<BankAccount>) -> Unit) {
        listenerRegistration = firestore.collection(collectionName)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to bank accounts", error)
                    return@addSnapshotListener
                }
                
                if (querySnapshot != null) {
                    val accounts = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            BankAccount(
                                id = doc.id,
                                bankName = doc.getString("bankName") ?: "",
                                accountNumber = doc.getString("accountNumber") ?: "",
                                branchCode = doc.getString("branchCode") ?: "",
                                iconResId = doc.getLong("iconResId")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing bank account: ${doc.id}", e)
                            null
                        }
                    }
                    
                    // Update local cache
                    localDatabase.saveBankAccounts(accounts)
                    onUpdate(accounts)
                    
                    Log.d(TAG, "Real-time update: ${accounts.size} bank accounts")
                }
            }
    }
    
    /**
     * Stop the real-time listener
     */
    fun stopRealtimeListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d(TAG, "Real-time listener stopped")
    }
    
    /**
     * Get bank accounts from local cache (for offline access)
     */
    fun getBankAccountsFromCache(): List<BankAccount> {
        return localDatabase.getBankAccounts()
    }
}

