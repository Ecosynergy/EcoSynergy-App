package br.ecosynergy_app.signup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.ecosynergy_app.R
import br.ecosynergy_app.RetrofitClient
import br.ecosynergy_app.home.HomeActivity
import br.ecosynergy_app.login.LoginActivity
import br.ecosynergy_app.login.LoginRequest
import br.ecosynergy_app.room.AppDatabase
import br.ecosynergy_app.room.teams.MembersRepository
import br.ecosynergy_app.room.teams.TeamsRepository
import br.ecosynergy_app.room.user.UserRepository
import br.ecosynergy_app.teams.RoleRequest
import br.ecosynergy_app.teams.TeamsViewModel
import br.ecosynergy_app.teams.TeamsViewModelFactory
import br.ecosynergy_app.user.UserViewModel
import br.ecosynergy_app.user.UserViewModelFactory

class EmailConfirmationActivity : AppCompatActivity() {

    private lateinit var signUpViewModel: SignUpViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var teamsViewModel: TeamsViewModel
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var overlayView: View

    private lateinit var email: String
    private lateinit var password: String
    private lateinit var fullName: String
    private lateinit var username: String
    private lateinit var nationality: String
    private lateinit var gender: String

    private var digit1Text: String = ""
    private var digit2Text: String = ""
    private var digit3Text: String = ""
    private var digit4Text: String = ""
    private var digit5Text: String = ""
    private var digit6Text: String = ""

    private lateinit var txtEmailShow: TextView

    private var userId: Int = 0

    private var verificationCode: String = ""

    private lateinit var loginSp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emailconfirmation)

        loginSp = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        val txtResend = findViewById<TextView>(R.id.txtResend)
        val digit1 = findViewById<EditText>(R.id.digit1)
        val digit2 = findViewById<EditText>(R.id.digit2)
        val digit3 = findViewById<EditText>(R.id.digit3)
        val digit4 = findViewById<EditText>(R.id.digit4)
        val digit5 = findViewById<EditText>(R.id.digit5)
        val digit6 = findViewById<EditText>(R.id.digit6)

        txtEmailShow = findViewById(R.id.txtEmailShow)

        val txtError = findViewById<LinearLayout>(R.id.txtError)

        val btnCheck: Button = findViewById(R.id.btnCheck)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        email = intent.getStringExtra("EMAIL").toString()
        password = intent.getStringExtra("PASSWORD").toString()
        fullName = intent.getStringExtra("FULLNAME").toString()
        username = intent.getStringExtra("USERNAME").toString()
        nationality = intent.getStringExtra("NATIONALITY").toString()
        gender = intent.getStringExtra("GENDER").toString()

        txtEmailShow.text = "Digite o código que enviamos ao e-mail informado: $email"

        val userDao = AppDatabase.getDatabase(applicationContext).userDao()
        val userRepository = UserRepository(userDao)

        val teamsDao = AppDatabase.getDatabase(applicationContext).teamsDao()
        val teamsRepository = TeamsRepository(teamsDao)

        val membersDao = AppDatabase.getDatabase(applicationContext).membersDao()
        val membersRepository = MembersRepository(membersDao)

        userViewModel = ViewModelProvider(
            this,
            UserViewModelFactory(RetrofitClient.userService, userRepository)
        )[UserViewModel::class.java]
        teamsViewModel = ViewModelProvider(
            this,
            TeamsViewModelFactory(RetrofitClient.teamsService, teamsRepository, membersRepository)
        )[TeamsViewModel::class.java]
        signUpViewModel = ViewModelProvider(
            this,
            SignUpViewModelFactory(RetrofitClient.signUpService)
        )[SignUpViewModel::class.java]

        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        overlayView = findViewById(R.id.overlayView)

        showProgressBar(true)

        btnBack.setOnClickListener { finish() }

        confirmationCode(email)

        btnCheck.setOnClickListener {
            digit1Text = digit1.text.toString()
            digit2Text = digit2.text.toString()
            digit3Text = digit3.text.toString()
            digit4Text = digit4.text.toString()
            digit5Text = digit5.text.toString()
            digit6Text = digit6.text.toString()

            if (verificationCode.length == 6) {
                val isCodeMatched =
                    digit1Text == verificationCode[0].toString() &&
                            digit2Text == verificationCode[1].toString() &&
                            digit3Text == verificationCode[2].toString() &&
                            digit4Text == verificationCode[3].toString() &&
                            digit5Text == verificationCode[4].toString() &&
                            digit6Text == verificationCode[5].toString()

                if (isCodeMatched) {
                    txtError.visibility = View.INVISIBLE
                    showProgressBar(true)

                    val createUserRequest =
                        CreateUserRequest(username, fullName, email, password, gender, nationality)
                    registerUser(createUserRequest)
                } else {
                    txtError.visibility = View.VISIBLE
                }
            } else {
                LoginActivity().showToast("ERRO NO ENVIO DO CÓDIGO", this)
            }
        }

        txtResend.setOnClickListener {
            txtResend.isClickable = false
            txtResend.setTextColor(Color.GRAY)
            startCountDown(txtResend)
            confirmationCode(email)
        }

        setEditTextFocusChange(digit1, digit2, null)
        setEditTextFocusChange(digit2, digit3, digit1)
        setEditTextFocusChange(digit3, digit4, digit2)
        setEditTextFocusChange(digit4, digit5, digit3)
        setEditTextFocusChange(digit5, digit6, digit4)
        setEditTextFocusChange(digit6, digit6, digit5)


        digitListener(digit1)
        digitListener(digit2)
        digitListener(digit3)
        digitListener(digit4)
        digitListener(digit5)
        digitListener(digit6)
    }

    fun digitListener(textView: EditText){
        textView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val uppercaseText = s.toString().uppercase()

                if (s.toString() != uppercaseText) {
                    textView.setText(uppercaseText)
                    textView.setSelection(uppercaseText.length)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun registerUser(createUserRequest: CreateUserRequest) {
        signUpViewModel.registerUser(createUserRequest)

        signUpViewModel.registerResult.observe(this) { result ->
            result.onSuccess { response ->
                val loginRequest = LoginRequest(username, password)
                userViewModel.loginUser(loginRequest)
                userId = response.id
            }.onFailure { error ->
                error.printStackTrace()
                Log.d("ConfirmationActivity", "Register failed: ${error.message}")
            }
        }

        userViewModel.loginResult.observe(this) { result ->
            showProgressBar(false)
            result.onSuccess { loginResponse ->
                Log.d("ConfirmationActivity", "Login success")
                teamsViewModel.addMember(
                    loginResponse.accessToken,
                    89,
                    userId,
                    RoleRequest("COMMON_USER")
                )
                userViewModel.user.observe(this) { result ->
                    result.onSuccess { userData ->
                        userViewModel.insertUserInfoDB(
                            userData,
                            loginResponse.accessToken,
                            loginResponse.refreshToken
                        )
                        teamsViewModel.getTeamsByUserId(userData.id, loginResponse.accessToken) {
                            setLoggedIn(true)
                            startHomeActivity()
                            loginSp.edit().apply {
                                putBoolean("just_logged_in", true)
                                putBoolean("open", true)
                                apply()
                            }
                        }
                    }
                    userViewModel.user.removeObservers(this)
                }
            }.onFailure { error ->
                error.printStackTrace()
                Log.d("ConfirmationActivity", "Login failed: ${error.message}")
            }
        }
    }

    private fun showProgressBar(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        overlayView.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun startCountDown(textView: TextView) {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                textView.text = "Reenvie em ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                textView.text = "Reenviar código"
                textView.isClickable = true
                textView.setTextColor(resources.getColor(R.color.greenDark, null))
            }
        }.start()
    }

    fun setEditTextFocusChange(currentEditText: EditText, nextEditText: EditText, previousEditText: EditText? = null) {
        currentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    nextEditText.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        currentEditText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN && currentEditText.text.isEmpty()) {
                previousEditText?.requestFocus()  // Move focus back to the previous EditText
            }
            false
        }
    }

    private fun startHomeActivity() {
        val i = Intent(this, HomeActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    private fun setLoggedIn(isLoggedIn: Boolean) {
        val editor = loginSp.edit()
        editor.putBoolean("isLoggedIn", isLoggedIn)
        editor.apply()
    }

    private fun confirmationCode(userEmail: String) {
        signUpViewModel.verificationCode.removeObservers(this)
        signUpViewModel.confirmationCode(userEmail)
        signUpViewModel.verificationCode.observe(this) { code ->
            Log.d("ConfirmationActivity", "Confirmation code: $code")
            if (code != null) {
                verificationCode = code
            }

            showProgressBar(false)
        }
    }
}
