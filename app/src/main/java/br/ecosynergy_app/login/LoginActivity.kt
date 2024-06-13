package br.ecosynergy_app.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.autofill.UserData
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AppCompatActivity
import br.ecosynergy_app.R
import br.ecosynergy_app.RetrofitClient
import br.ecosynergy_app.home.HomeActivity
import br.ecosynergy_app.register.RegisterActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var txtEntry: EditText
    private lateinit var txtPassword: TextInputEditText
    private lateinit var authViewModel: AuthViewModel
    private lateinit var lblReset: TextView
    private var hasErrorShown = false
    private lateinit var oneTapClient: SignInClient
    private lateinit var auth: FirebaseAuth
    private val REQ_ONE_TAP = 2 // Can be any integer unique to the Activity
    private var showOneTapUI = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (isLoggedIn()) {
            startHomeActivity()
            return
        }

        authViewModel = ViewModelProvider(this, AuthViewModelFactory(RetrofitClient.authService)).get(AuthViewModel::class.java)

        txtEntry = findViewById(R.id.txtEntry)
        txtPassword = findViewById(R.id.txtPassword)
        val passwordLayout: TextInputLayout = findViewById(R.id.passwordLayout)
        val btnLogin: Button = findViewById(R.id.btnLogin)
        val btnRegister: Button = findViewById(R.id.btnRegister)
        val btnGoogle: SignInButton = findViewById(R.id.btnGoogle)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)

        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.your_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            .build()

        btnGoogle.setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender, REQ_ONE_TAP,
                            null, 0, 0, 0, null)
                    } catch (e: Exception) {
                        showToast("Failed to start sign-in: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(this) { e ->
                    showToast("Failed to start sign-in: ${e.localizedMessage}")
                }
        }


        btnLogin.setOnClickListener {
            val username = txtEntry.text.toString()
            val password = txtPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                if (username.isEmpty()) {
                    txtEntry.error = "Insira seu Nome de Usuário"
                }
                if (password.isEmpty()) {
                    passwordLayout.endIconMode = TextInputLayout.END_ICON_NONE
                    txtPassword.error = "Insira sua senha"
                    hasErrorShown = true
                }
                return@setOnClickListener
            }
            val loginRequest = LoginRequest(username, password)
            authViewModel.loginUser(loginRequest)
        }

        txtPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    passwordLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    passwordLayout.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (hasErrorShown && s.isNullOrEmpty()) {
                    passwordLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    hasErrorShown = false
                }
            }
        })

        btnRegister.setOnClickListener {
            val i = Intent(this, RegisterActivity::class.java)
            startActivity(i)
        }

        authViewModel.loginResult.observe(this) { result ->
            result.onSuccess { loginResponse ->
                Log.d("LoginActivity", "Login success")
                setLoggedIn(true, loginResponse.username, loginResponse.accessToken)
                startHomeActivity()
            }.onFailure { error ->
                error.printStackTrace()
                Log.d("LoginActivity", "Login failed: ${error.message}")
                txtEntry.error = "Usuário/Email ou Senha incorreto! Por favor verifique seus dados"
                txtPassword.text = null
                txtEntry.requestFocus()
            }
        }

        lblReset = findViewById(R.id.lblReset)

        lblReset.setOnClickListener(){
            val i = Intent(this, ResetActivity::class.java)
            startActivity(i)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_ONE_TAP) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                when {
                    idToken != null -> {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    user?.let {
                                        val userData = GoogleUserData(
                                            uid = it.uid,
                                            email = it.email ?: "",
                                            displayName = it.displayName ?: ""
                                        )
                                        handleUserSignIn(userData)
                                    }
                                } else {
                                    showToast("Firebase sign-in failed: ${task.exception?.localizedMessage}")
                                    updateUI(null)
                                }
                            }
                    }
                    else -> {
                        showToast("No ID token!")
                    }
                }
            } catch (e: ApiException) {
                showToast("Sign-in failed: ${e.localizedMessage}")
            }
        }
    }

    private fun handleUserSignIn(userData: GoogleUserData) {
        authViewModel.checkUserExistence(userData.email).observe(this) { response ->
            if (response.isSuccessful) {
                val existingUser = response.body()
                if (existingUser == null) {
                    // User doesn't exist, register new user
                    authViewModel.registerUser(userData).observe(this) { registrationResponse ->
                        if (registrationResponse.isSuccessful) {
                            setLoggedIn(true, userData.email, registrationResponse.body()?.accessToken)
                            startHomeActivity()
                        } else {
                            showToast("Registration failed: ${registrationResponse.message()}")
                        }
                    }
                } else {
                    // User exists, log them in
                    setLoggedIn(true, existingUser.email, existingUser.accessToken)
                    startHomeActivity()
                }
            } else {
                showToast("User check failed: ${response.message()}")
            }
        }
    }


    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startHomeActivity()
        } else {
            showToast("Authentication failed.")
        }
    }


    private fun startHomeActivity() {
        val i = Intent(this, HomeActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setLoggedIn(isLoggedIn: Boolean, username: String? = null, accessToken: String? = null) {
        val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", isLoggedIn)
        if (username != null) {
            editor.putString("username", username)
        }
        if (accessToken != null) {
            editor.putString("accessToken", accessToken)
        }
        editor.apply()
    }

    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }
}
