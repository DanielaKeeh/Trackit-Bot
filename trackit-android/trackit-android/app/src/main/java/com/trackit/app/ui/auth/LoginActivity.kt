package com.trackit.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.trackit.app.data.ServiceLocator
import com.trackit.app.data.repository.ApiResult
import com.trackit.app.data.repository.UserRepository
import com.trackit.app.databinding.ActivityLoginBinding
import com.trackit.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userRepository: UserRepository
    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = ServiceLocator.userRepository(this)

        if (userRepository.isLoggedIn()) {
            goToMain()
            return
        }

        binding.buttonSubmit.setOnClickListener { submit() }
        binding.textToggleMode.setOnClickListener { toggleMode() }
    }

    private fun toggleMode() {
        isRegisterMode = !isRegisterMode
        binding.buttonSubmit.text = getString(
            if (isRegisterMode) com.trackit.app.R.string.action_register else com.trackit.app.R.string.action_login
        )
        binding.textToggleMode.text = getString(
            if (isRegisterMode) com.trackit.app.R.string.toggle_to_login else com.trackit.app.R.string.toggle_to_register
        )
        binding.textError.visibility = View.GONE
    }

    private fun submit() {
        val username = binding.editUsername.text.toString().trim()
        val password = binding.editPassword.text.toString()

        if (username.isBlank() || password.isBlank()) {
            showError(getString(com.trackit.app.R.string.error_fields_required))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = if (isRegisterMode) {
                userRepository.register(username, password)
            } else {
                userRepository.login(username, password)
            }
            setLoading(false)

            when (result) {
                is ApiResult.Success -> goToMain()
                is ApiResult.Failure -> showError(result.message)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSubmit.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
