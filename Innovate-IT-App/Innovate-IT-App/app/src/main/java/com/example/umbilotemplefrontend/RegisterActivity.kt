package com.example.umbilotemplefrontend

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

class RegisterActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "RegisterActivity"
    }
    
    private lateinit var etName: EditText
    private lateinit var etEmailAddress: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnSignUp: Button
    private lateinit var btnGoogleSignUp: ImageButton
    private lateinit var tvLogin: TextView
    private lateinit var tvPasswordStrength: TextView
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
        
        setContentView(R.layout.activity_register)
        
        // Initialize database and repository
        localDatabase = LocalDatabase(this)
        userRepository = UserRepository(this)
        
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
        etName = findViewById(R.id.etName)
        etEmailAddress = findViewById(R.id.etEmailAddress)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp)
        tvLogin = findViewById(R.id.tvLogin)
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength)
        progressBar = findViewById(R.id.progressBar)
        
        // Set password transformation method
        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        
        // Set up password strength meter
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isNotEmpty()) {
                    updatePasswordStrengthView(password)
                } else {
                    tvPasswordStrength.visibility = View.GONE
                }
            }
        })
        
        // Set click listeners
        btnSignUp.setOnClickListener {
            registerWithEmailPassword()
        }
        
        btnGoogleSignUp.setOnClickListener {
            signUpWithGoogle()
        }
        
        tvLogin.setOnClickListener {
            navigateToLogin()
        }
    }
    
    private fun updatePasswordStrengthView(password: String) {
        val strength = AuthValidator.getPasswordStrength(password)
        tvPasswordStrength.visibility = View.VISIBLE
        
        when (strength) {
            AuthValidator.PasswordStrength.WEAK -> {
                tvPasswordStrength.text = "Weak Password"
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            AuthValidator.PasswordStrength.MEDIUM -> {
                tvPasswordStrength.text = "Medium Password"
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            AuthValidator.PasswordStrength.STRONG -> {
                tvPasswordStrength.text = "Strong Password"
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
        }
        
        // Show specific error message if available
        val errorMessage = AuthValidator.getPasswordErrorMessage(password)
        if (errorMessage != null) {
            tvPasswordStrength.text = errorMessage
            tvPasswordStrength.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }
    
    private fun registerWithEmailPassword() {
        val name = etName.text.toString().trim()
        val email = etEmailAddress.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        
        // Validate input
        if (!validateRegistrationInput(name, email, password, confirmPassword, phoneNumber)) {
            return
        }
        
        // Show progress
        showProgress(true)
        
        // Check if email already exists locally
        val existingUser = localDatabase.getUserByEmail(email)
        if (existingUser != null) {
            Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_SHORT).show()
            showProgress(false)
            return
        }
        
        // Check if email already exists in Firebase
        firebaseAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { methodsTask ->
                if (methodsTask.isSuccessful) {
                    val methods = methodsTask.result?.signInMethods ?: emptyList()
                    if (methods.isNotEmpty()) {
                        Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_LONG).show()
                        showProgress(false)
                        return@addOnCompleteListener
                    }
                    
                    // Email doesn't exist, proceed with registration
                    proceedWithRegistration(name, email, password, phoneNumber)
                } else {
                    // If Firebase check fails, still check Firestore as fallback
                    firestore.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnCompleteListener { firestoreTask ->
                            if (firestoreTask.isSuccessful && !firestoreTask.result?.isEmpty!!) {
                                Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_LONG).show()
                                showProgress(false)
                            } else {
                                // Email doesn't exist, proceed with registration
                                proceedWithRegistration(name, email, password, phoneNumber)
                            }
                        }
                }
            }
    }
    
    private fun proceedWithRegistration(name: String, email: String, password: String, phoneNumber: String) {
        val newUser = localDatabase.registerUser(name, email, password, phoneNumber)
        if (newUser != null) {
            localDatabase.setCurrentUser(newUser)
            
            // IMPORTANT: Sign in to Firebase Auth for Storage access
            firebaseAuth.signInAnonymously()
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Log.d(TAG, "Firebase Auth anonymous sign-in successful")
                    } else {
                        Log.e(TAG, "Firebase Auth anonymous sign-in failed", authTask.exception)
                    }
                    
                    // Save to Firebase
                    userRepository.saveUserToFirestore(
                        newUser,
                        onSuccess = {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        },
                        onError = { exception ->
                            Log.e(TAG, "Error syncing user to Firebase", exception)
                            // Still proceed with local registration
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        }
                    )
                }
        } else {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
            showProgress(false)
        }
    }
    
    private fun validateRegistrationInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        phoneNumber: String
    ): Boolean {
        // Check if name is empty
        if (name.isEmpty()) {
            etName.error = "Name is required"
            etName.requestFocus()
            return false
        }
        
        // Check if name is valid
        if (!AuthValidator.isNameValid(name)) {
            etName.error = "Please enter a valid name (2-30 letters only)"
            etName.requestFocus()
            return false
        }
        
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
        
        // Check if password is valid
        if (!AuthValidator.isPasswordValidBasic(password)) {
            etPassword.error = "Password must be at least 8 characters"
            etPassword.requestFocus()
            return false
        }
        
        // Check if confirm password is empty
        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Please confirm your password"
            etConfirmPassword.requestFocus()
            return false
        }
        
        // Check if passwords match
        if (!AuthValidator.doPasswordsMatch(password, confirmPassword)) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return false
        }
        
        // Check if phone number is valid (if provided)
        if (phoneNumber.isNotEmpty() && !AuthValidator.isPhoneNumberValid(phoneNumber)) {
            etPhoneNumber.error = "Please enter a valid phone number"
            etPhoneNumber.requestFocus()
            return false
        }
        
        return true
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
    
    private fun signUpWithGoogle() {
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
            
            // Get account details (with safe fallbacks)
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
            
            // Prevent duplicate signup
            firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { methodsTask ->
                    if (methodsTask.isSuccessful) {
                        val methods = methodsTask.result?.signInMethods ?: emptyList()
                        if (methods.isNotEmpty()) {
                            Toast.makeText(this, "User already registered. Please login.", Toast.LENGTH_LONG).show()
                            showProgress(false)
                            return@addOnCompleteListener
                        }

                        // First-time signup: sign in to Firebase with Google credential
                        val idToken = account?.idToken
                        if (idToken.isNullOrBlank()) {
                            Toast.makeText(this, "Missing Google ID token", Toast.LENGTH_SHORT).show()
                            showProgress(false)
                            return@addOnCompleteListener
                        }
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        firebaseAuth.signInWithCredential(credential)
                            .addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    val uid = firebaseAuth.currentUser?.uid.orEmpty()
                                    val data = mapOf(
                                        "uid" to uid,
                                        "name" to name,
                                        "email" to email,
                                        "provider" to "google"
                                    )
                                    firestore.collection("users").document(uid).set(data)
                                        .addOnFailureListener { e -> Log.w(TAG, "Firestore upsert failed", e) }

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
                    } else {
                        Log.w(TAG, "fetchSignInMethodsForEmail failed", methodsTask.exception)
                        Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
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
    
    private fun navigateToLogin() {
        finish() // Go back to the login screen
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSignUp.isEnabled = !show
        btnGoogleSignUp.isEnabled = !show
        tvLogin.isEnabled = !show
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
} 