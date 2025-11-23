package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    companion object {
        private const val TAG = "UserViewModel"
    }

    // Login state
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    // Current user
    private val _currentUser = MutableStateFlow<Entity_Users?>(null)
    val currentUser: StateFlow<Entity_Users?> = _currentUser

    init {
        android.util.Log.d(TAG, "🚀 UserViewModel initialized")
    }

    // ============ LOGIN FUNCTION ============

    fun loginUser(username: String, password: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d(TAG, "🔐 Attempting login for: $username")

                _loginState.value = LoginState.Loading

                val user = repository.loginUser(username, password)

                if (user != null) {
                    _currentUser.value = user
                    UserSession.currentUser = user
                    _loginState.value = LoginState.Success(user)

                    android.util.Log.d(TAG, "✅ Login successful!")
                    android.util.Log.d(TAG, "   User: ${user.Entity_fname} ${user.Entity_lname}")
                    android.util.Log.d(TAG, "   Role: ${user.role}")
                    android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                } else {
                    _loginState.value = LoginState.Error("Invalid username or password")
                    android.util.Log.w(TAG, "❌ Login failed - invalid credentials")
                    android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
                android.util.Log.e(TAG, "❌ Login error: ${e.message}", e)
                android.util.Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
    }

    // ============ LOGOUT FUNCTION ============

    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
                _currentUser.value = null
                UserSession.logout()
                _loginState.value = LoginState.Idle
                android.util.Log.d(TAG, "✅ Logged out successfully")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Logout error: ${e.message}")
            }
        }
    }

    // ============ RESET LOGIN STATE ============

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}

// ============ LOGIN STATE SEALED CLASS ============

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: Entity_Users) : LoginState()
    data class Error(val message: String) : LoginState()
}

// ============ VIEW MODEL FACTORY ============

class UserViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}