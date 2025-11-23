package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class UserRepository(
    private val daoUsers: Dao_Users
) {
    companion object {
        private const val TAG = "UserRepository"
    }

    // ============ SYNC USERS FROM API ============

    suspend fun syncUsersFromFirebase(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📡 Syncing users from API...")

                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.getAllUsers()
                }

                if (result.isSuccess) {
                    val users = result.getOrNull() ?: return@withContext Result.success(Unit)
                    Log.d(TAG, "✅ API returned ${users.size} users")

                    val usersList = users.map { userResponse ->
                        Entity_Users(
                            Entity_id = 0,
                            Entity_lname = userResponse.lname,
                            Entity_fname = userResponse.fname,
                            Entity_mname = userResponse.mname,
                            Entity_username = userResponse.username,
                            role = userResponse.role,
                            status = userResponse.status,
                            joinedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        )
                    }

                    Log.d(TAG, "✅ Parsed ${usersList.size} users from API")

                    if (usersList.isNotEmpty()) {
                        usersList.forEach { user ->
                            val existingUser = daoUsers.getUserByUsername(user.Entity_username)
                            if (existingUser != null) {
                                val updatedUser = user.copy(Entity_id = existingUser.Entity_id)
                                daoUsers.DaoUpdate(updatedUser)
                                Log.d(TAG, "♻️ Updated user: ${user.Entity_username} (${user.role})")
                            } else {
                                daoUsers.DaoInsert(user)
                                Log.d(TAG, "➕ Added new user: ${user.Entity_username} (${user.role})")
                            }
                        }
                        Log.d(TAG, "✅ Synced to Room database")
                    }

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.success(Unit)
                } else {
                    Log.w(TAG, "⚠️ API sync failed: ${result.exceptionOrNull()?.message}")
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ User sync failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ============ LOGIN WITH API ============

    suspend fun loginUser(username: String, password: String): Entity_Users? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔐 Attempting login...")
                Log.d(TAG, "Username: $username")

                // Call API login
                val request = LoginRequest(username = username)
                val result = BaneloApiService.safeCall {
                    BaneloApiService.api.login(request)
                }

                if (result.isSuccess) {
                    val userResponse = result.getOrNull()
                    if (userResponse != null) {
                        Log.d(TAG, "✅ Login successful: ${userResponse.fname} ${userResponse.lname}")
                        Log.d(TAG, "   Role: ${userResponse.role}")
                        Log.d(TAG, "   Status: ${userResponse.status}")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")

                        return@withContext Entity_Users(
                            Entity_id = 0,
                            Entity_lname = userResponse.lname,
                            Entity_fname = userResponse.fname,
                            Entity_mname = userResponse.mname,
                            Entity_username = userResponse.username,
                            role = userResponse.role,
                            status = userResponse.status,
                            joinedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        )
                    }
                } else {
                    Log.e(TAG, "❌ Login failed: ${result.exceptionOrNull()?.message}")
                }

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Login failed: ${e.message}", e)
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                null
            }
        }
    }

    // ============ LOGOUT ============

    fun logout() {
        try {
            Log.d(TAG, "✅ Logged out")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Logout error: ${e.message}")
        }
    }
}
