package com.project.dba_delatorre_dometita_ramirez_tan

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Login(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userDao = Database_Users.getDatabase(context).dao_users()

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0E1CC), Color(0xFFD18F79))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.beans),
                contentDescription = "Beans",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 280.dp)
        ) {
            Card(
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        Text("Welcome Back", fontSize = 24.sp, color = Color(0xFF6B3E2E))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Login to continue", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            enabled = !isLoading
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            enabled = !isLoading,
                            trailingIcon = {
                                val icon = if (passwordVisible)
                                    painterResource(id = R.drawable.ic_visibility_off)
                                else
                                    painterResource(id = R.drawable.ic_visibility)

                                val description =
                                    if (passwordVisible) "Hide password" else "Show password"

                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(painter = icon, contentDescription = description)
                                }
                            }
                        )
                        if (isLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF6B3E2E)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Authenticating...",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Button at bottom
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    when {
                                        username.isBlank() || password.isBlank() -> {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Please enter both username and password.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                        else -> {
                                            isLoading = true

                                            try {
                                                // ✅ Initialize AuditHelper (safe to call multiple times)
                                                AuditHelper.initialize(context)

                                                // ✅ Use UserRepository for Firebase Auth login
                                                val userRepository = UserRepository(userDao)
                                                val user = userRepository.loginUser(username, password)

                                                withContext(Dispatchers.Main) {
                                                    isLoading = false

                                                    if (user != null) {
                                                        // ✅ Save user session
                                                        UserSession.currentUser = user

                                                        // ✅ Get username and full name
                                                        val loggedUsername = user.Entity_username
                                                        val fullName = "${user.Entity_fname} ${user.Entity_lname}"
                                                        val userRole = user.role

                                                        // ✅ Log successful login with BOTH parameters
                                                        AuditHelper.logLogin(loggedUsername, fullName)
                                                        android.util.Log.d("Login", "✅ Audit trail logged for login: $loggedUsername ($fullName)")
                                                        android.util.Log.d("Login", "👤 User role: $userRole")

                                                        // ✅ Get role-based default route
                                                        val defaultRoute = RoleManager.getDefaultRoute()
                                                        android.util.Log.d("Login", "🎯 Navigating to: $defaultRoute")

                                                        Toast.makeText(
                                                            context,
                                                            "Welcome ${user.Entity_fname}! (${user.role})",
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                        // ✅ Navigate to role-appropriate screen
                                                        navController.navigate(defaultRoute) {
                                                            popUpTo(Routes.R_Login.routes) { inclusive = true }
                                                        }
                                                    } else {
                                                        // ✅ Log failed login attempt
                                                        AuditHelper.logFailedLogin(username)
                                                        android.util.Log.d("Login", "❌ Failed login attempt for: $username")

                                                        Toast.makeText(
                                                            context,
                                                            "Invalid username or password.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false

                                                    // ✅ Log failed login attempt
                                                    AuditHelper.logFailedLogin(username)
                                                    android.util.Log.e("Login", "❌ Login error: ${e.message}", e)

                                                    Toast.makeText(
                                                        context,
                                                        "Login failed: ${e.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6B3E2E),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Login", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}