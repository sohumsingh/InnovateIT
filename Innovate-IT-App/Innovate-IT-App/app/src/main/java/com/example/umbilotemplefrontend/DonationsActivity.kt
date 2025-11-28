package com.example.umbilotemplefrontend

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.umbilotemplefrontend.models.BankAccount
import com.example.umbilotemplefrontend.utils.BankAccountRepository
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.NavigationHandler

class DonationsActivity : AppCompatActivity() {
    
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var localDatabase: LocalDatabase
    private lateinit var bankAccountRepository: BankAccountRepository
    private lateinit var bankAccountsContainer: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donations)
        
        // Initialize database and repository
        localDatabase = LocalDatabase(this)
        bankAccountRepository = BankAccountRepository(this)
        
        // Initialize navigation handler
        navigationHandler = NavigationHandler(this)
        
        // Check if user is logged in
        if (!navigationHandler.checkUserLoggedIn()) {
            return
        }
        
        // Set up navigation
        navigationHandler.setupNavigation()
        
        // Initialize views
        bankAccountsContainer = findViewById(R.id.bankAccountsContainer)
        
        // Set up back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            // Navigate to home page instead of closing app
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        // Load bank accounts
        loadBankAccounts()
    }
    
    private fun loadBankAccounts() {
        // Try to fetch from Firestore first, fall back to local cache
        bankAccountRepository.fetchBankAccountsFromFirestore(
            onSuccess = { accounts ->
                displayBankAccounts(accounts)
            },
            onError = { exception ->
                // Use local cache
                val cachedAccounts = bankAccountRepository.getBankAccountsFromCache()
                displayBankAccounts(cachedAccounts)
                Toast.makeText(
                    this,
                    "Using cached bank accounts (offline mode)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
    
    private fun displayBankAccounts(accounts: List<BankAccount>) {
        bankAccountsContainer.removeAllViews()
        
        if (accounts.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No bank accounts available"
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.white, null))
                setPadding(16, 16, 16, 16)
            }
            bankAccountsContainer.addView(emptyView)
            return
        }
        
        for (account in accounts) {
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_bank_card, bankAccountsContainer, false)
            
            // Populate card data
            cardView.findViewById<TextView>(R.id.tvBankName).text = account.bankName
            cardView.findViewById<TextView>(R.id.tvAccountNumber).text = account.accountNumber
            cardView.findViewById<TextView>(R.id.tvBranchCode).text = account.branchCode
            
            // Set up copy buttons
            cardView.findViewById<ImageButton>(R.id.btnCopyAccountNumber).setOnClickListener {
                copyToClipboard("Account Number", account.accountNumber)
            }
            
            cardView.findViewById<ImageButton>(R.id.btnCopyBranchCode).setOnClickListener {
                copyToClipboard("Branch Code", account.branchCode)
            }
            
            bankAccountsContainer.addView(cardView)
        }
    }
    
    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop real-time listener if it was started
        bankAccountRepository.stopRealtimeListener()
    }
} 