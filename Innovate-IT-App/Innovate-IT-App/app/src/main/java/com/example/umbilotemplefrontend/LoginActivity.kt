package com.example.umbilotemplefrontend

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.umbilotemplefrontend.models.User
import com.example.umbilotemplefrontend.utils.AuthValidator
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 9001
    }
    
    private lateinit var etEmailAddress: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: ImageButton
    private lateinit var tvSignUp: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var localDatabase: LocalDatabase
    private lateinit var userRepository: UserRepository
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_login)
        
        // Initialize database and repository
        localDatabase = LocalDatabase(this)
        userRepository = UserRepository(this)
        
        // Check if user is already logged in
        if (localDatabase.isLoggedIn()) {
            val currentUser = localDatabase.getCurrentUser()
            if (currentUser != null) {
                Log.d(TAG, "User already logged in: ${currentUser.email}, isAdmin: ${currentUser.isAdmin}")
                
                // Ensure admin status is preserved
                if (currentUser.isAdmin) {
                    Log.d(TAG, "Ensuring admin user has correct privileges")
                }
                
                // IMPORTANT: Sign in to Firebase Auth for Storage access
                firebaseAuth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Firebase Auth anonymous sign-in successful for existing user")
                        } else {
                            Log.e(TAG, "Firebase Auth anonymous sign-in failed for existing user", task.exception)
                        }
                    }
            }
            navigateToHome()
            return
        }
        
        // Configure Google Sign-In
        configureGoogleSignIn()
        
        // Register the ActivityResultLauncher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            } catch (e: Exception) {
                if (e is ApiException) {
                    Log.w(TAG, "Google sign-in failed in callback: code=${e.statusCode}, message=${e.message}")
                    Toast.makeText(this, "Google sign-in failed (code ${e.statusCode})", Toast.LENGTH_LONG).show()
                } else {
                    Log.w(TAG, "Google sign-in failed in callback: ${e.message}")
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
                showProgress(false)
            }
        }
        
        // Initialize views
        etEmailAddress = findViewById(R.id.etEmailAddress)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        progressBar = findViewById(R.id.progressBar)
        
        // Set password transformation method
        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        
        // Set click listeners
        btnLogin.setOnClickListener {
            loginWithEmailPassword()
        }
        
        btnGoogleLogin.setOnClickListener {
            loginWithGoogle()
        }
        
        tvSignUp.setOnClickListener {
            navigateToRegister()
        }
        
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }
    
    private fun configureGoogleSignIn() {
        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
            
        // Build a GoogleSignInClient with the options
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }
    
    
    
    private fun loginWithEmailPassword() {
        val email = etEmailAddress.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        // Debug: Check for admin login attempt
        if (email == "TestingAdmin@gmail.com" && password == "Testing@1") {
            // Check if user exists
            val existingUser = localDatabase.getUserByEmail(email)
            if (existingUser == null) {
                // Create admin user directly if it doesn't exist
                val adminUser = localDatabase.registerUser(
                    name = "Testing",
                    email = "TestingAdmin@gmail.com",
                    password = "Testing@1",
                    phoneNumber = "066 025 1636",
                    isAdmin = true
                )
                
                if (adminUser != null) {
                    // Sync admin user to Firebase
                    userRepository.saveUserToFirestore(
                        adminUser,
                        onSuccess = {
                            Log.d(TAG, "Admin user synced to Firebase")
                        },
                        onError = { e ->
                            Log.e(TAG, "Error syncing admin user to Firebase", e)
                        }
                    )
                    localDatabase.setCurrentUser(adminUser)
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                    return
                }
            }
        }
        
        // Validate input
        if (!validateLoginInput(email, password)) {
            return
        }
        
        // Show progress
        showProgress(true)
        
        // Debug: List all users in database
        val allUsers = localDatabase.getUsers()
        Log.d("LoginActivity", "All users in database: ${allUsers.size}")
        allUsers.forEach { user ->
            Log.d("LoginActivity", "User: ${user.email}, Admin: ${user.isAdmin}")
        }
        
        // Simulate network delay
        // Authenticate user immediately (no artificial delay)
        val user = localDatabase.authenticateUser(email, password)
        if (user != null) {
            localDatabase.setCurrentUser(user)
            
            // IMPORTANT: Sign in to Firebase Auth for Storage access
            // Use anonymous sign-in for email/password users to avoid requiring Firebase Auth account creation
            firebaseAuth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Firebase Auth anonymous sign-in successful")
                    } else {
                        Log.e(TAG, "Firebase Auth anonymous sign-in failed", task.exception)
                    }
                    // Continue with navigation regardless of Firebase Auth result
                    // (upload functionality will still work with modified rules if needed)
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                    showProgress(false)
                }
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            showProgress(false)
        }
    }
    
    private fun validateLoginInput(email: String, password: String): Boolean {
        // Check if email is empty
        if (email.isEmpty()) {
            etEmailAddress.error = "Email is required"
            etEmailAddress.requestFocus()
            return false
        }
        
        // Check if email is valid
        if (!AuthValidator.isEmailValid(email)) {
            etEmailAddress.error = "Please enter a valid email address"
            etEmailAddress.requestFocus()
            return false
        }
        
        // Check if password is empty
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun loginWithGoogle() {
        // Show progress
        showProgress(true)
        
        // Force account chooser by clearing the cached account before launching
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }
    
    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            // Get the Google account
            val account = completedTask.getResult(ApiException::class.java)
            
            // Get account details (fallback when email is missing)
            val rawEmail = account?.email?.trim() ?: ""
            val id = account?.id ?: ""
            val email = if (rawEmail.isNotBlank()) rawEmail else if (id.isNotBlank()) "${id}@google.local" else ""
            val name = account?.displayName?.takeIf { !it.isNullOrBlank() } ?: "Google User"
            
            Log.d(TAG, "Google Sign-In successful: email=$email id=$id name=$name")
            
            if (email.isBlank()) {
                Toast.makeText(this, "Google account missing email", Toast.LENGTH_SHORT).show()
                showProgress(false)
                return
            }
            
            // Sign in to Firebase with Google credential
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(this, "Missing Google ID token", Toast.LENGTH_SHORT).show()
                showProgress(false)
                return
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        // Upsert user in Firestore
                        val uid = firebaseAuth.currentUser?.uid.orEmpty()
                        val data = mapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "provider" to "google"
                        )
                        // Upsert in background; do not block navigation
                        firestore.collection("users").document(uid).set(data)
                            .addOnFailureListener { e -> Log.w(TAG, "Firestore upsert failed", e) }

                        // Also keep local session
                        var user = localDatabase.getUserByEmail(email)
                        if (user == null) {
                            user = localDatabase.registerUser(name, email, id.ifBlank { email }, "")
                            // Sync new user to Firebase
                            user?.let { newUser ->
                                userRepository.saveUserToFirestore(
                                    newUser,
                                    onSuccess = {
                                        Log.d(TAG, "User synced to Firebase")
                                    },
                                    onError = { e ->
                                        Log.e(TAG, "Error syncing user to Firebase", e)
                                    }
                                )
                            }
                        }
                        user?.let { localDatabase.setCurrentUser(it) }
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    } else {
                        Log.w(TAG, "Firebase signInWithCredential failed", authTask.exception)
                        Toast.makeText(this, "Failed to sign in with Firebase", Toast.LENGTH_SHORT).show()
                        showProgress(false)
                    }
                }
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason
            Log.w(TAG, "Google sign-in failed: code=${e.statusCode}, message=${e.message}")
            Toast.makeText(this, "Google sign-in failed (code ${e.statusCode})", Toast.LENGTH_LONG).show()
            showProgress(false)
        }
    }
    
    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val etForgotEmail = dialogView.findViewById<EditText>(R.id.etForgotEmail)
        
        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Reset") { _, _ ->
                val email = etForgotEmail.text.toString().trim()
                
                if (email.isEmpty() || !AuthValidator.isEmailValid(email)) {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Check if user exists
                val user = localDatabase.getUserByEmail(email)
                if (user == null) {
                    Toast.makeText(this, "No account found with this email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // In a real app, send a password reset email
                Toast.makeText(this, "Password reset instructions sent to your email", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnGoogleLogin.isEnabled = !show
        tvSignUp.isEnabled = !show
        tvForgotPassword.isEnabled = !show
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
} 