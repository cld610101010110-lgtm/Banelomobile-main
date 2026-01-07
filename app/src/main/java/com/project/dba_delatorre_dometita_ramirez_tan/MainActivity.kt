package com.project.dba_delatorre_dometita_ramirez_tan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit



class MainActivity : ComponentActivity(), ImageLoaderFactory {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)
        CloudinaryHelper.initialize(this)
        AuditHelper.initialize(this)
        android.util.Log.d("MainActivity", "✅ AuditHelper initialized")
        android.util.Log.d("MainActivity", "✅ Coil ImageLoader will be initialized")

        // 🆕 Schedule automatic expiration check worker for daily 6 AM
        scheduleExpirationCheckWorker()
        runExpirationCheckNow()

        enableEdgeToEdge()
        val db = Database_Users.getDatabase(applicationContext)
        val userdao = db.dao_users()
        val repo = RepositoryUsers(userdao)
        val factory = viewModel_Factory(repo)
        val userViewModel = ViewModelProvider(this, factory)[ViewModel_users::class.java]
        val db2 = Database_Products.getDatabase(applicationContext)
        val repository = ProductRepository(db2.dao_products(), db2.dao_salesReport())
        val productViewModel = ViewModelProvider(this, ProductViewModelFactory(repository))[ProductViewModel::class.java]
        val salesReportRepository = SalesReportRepository(db2.dao_salesReport())
        val salesReportViewModel = ViewModelProvider(this, SalesReportViewModelFactory(salesReportRepository, repository))[SalesReportViewModel::class.java]
        val recipeRepository = RecipeRepository(db2)
        val wasteLogRepository = WasteLogRepository(db2.daoWasteLog())

        setContent {

            val navController = rememberNavController()
            val recipeViewModel: RecipeViewModel = viewModel(  // ✅ ADD THIS
                factory = RecipeViewModelFactory(recipeRepository)
            )
            val wasteLogViewModel: WasteLogViewModel = viewModel(
                factory = WasteLogViewModelFactory(wasteLogRepository)
            )
            NavHost(navController = navController, startDestination = Routes.R_Logo.routes) {
                composable(Routes.R_DashboardScreen.routes) {
                    // ✅ Check access before showing screen
                    if (RoleManager.canAccessRoute(Routes.R_DashboardScreen.routes)) {
                        dashboard(navController = navController, viewModel = salesReportViewModel)
                    } else {
                        // Redirect to appropriate screen
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }

                composable(Routes.R_Login.routes){
                    Login(navController = navController)
                }

                composable(Routes.R_InventoryList.routes) {
                    if (RoleManager.canAccessRoute(Routes.R_InventoryList.routes)) {
                        InventoryListScreen(
                            navController = navController,
                            viewModel3 = productViewModel,
                            recipeViewModel = recipeViewModel  // ✅ ADD THIS
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }

                composable(Routes.R_AddProduct.routes) {
                    // ✅ Check access before showing screen
                    if (RoleManager.canAccessRoute(Routes.R_AddProduct.routes)) {
                        AddProductScreen(navController = navController, viewModel3 = productViewModel)
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }

                composable(Routes.R_InventoryTransfer.routes) {
                    if (RoleManager.canAccessRoute(Routes.R_InventoryTransfer.routes)) {
                        InventoryTransferScreen(
                            navController = navController,
                            productViewModel = productViewModel
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }

                composable(Routes.R_WasteMarking.routes) {
                    if (RoleManager.canAccessRoute(Routes.R_WasteMarking.routes)) {
                        WasteMarkingScreen(
                            navController = navController,
                            productViewModel = productViewModel,
                            wasteLogViewModel = wasteLogViewModel
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }

                composable(Routes.R_IngredientCostView.routes) {
                    if (RoleManager.canAccessRoute(Routes.R_IngredientCostView.routes)) {
                        IngredientCostViewScreen(
                            navController = navController,
                            productViewModel = productViewModel,
                            recipeViewModel = recipeViewModel
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }

                composable(Routes.R_SalesReport.routes) {
                    if (RoleManager.canAccessRoute(Routes.R_SalesReport.routes)) {
                        SalesReportScreen(
                            navController = navController,
                            viewModel = salesReportViewModel
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }

                composable(Routes.R_WasteReport.routes) {
                    if (RoleManager.canAccessRoute(Routes.R_WasteReport.routes)) {
                        WasteReportScreen(
                            navController = navController,
                            wasteLogViewModel = wasteLogViewModel
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }

                composable(Routes.R_Logo.routes){
                    WelcomeLogo(navController = navController)
                }

                composable("EditProductScreen/{firebaseId}") { backStackEntry ->
                    val firebaseId = backStackEntry.arguments?.getString("firebaseId") ?: ""

                    // ✅ Check access before showing screen
                    if (RoleManager.canAccessRoute("EditProductScreen/$firebaseId")) {
                        val products = productViewModel.productList
                        val product = products.find { it.firebaseId == firebaseId }

                        if (product != null) {
                            EditProductScreen(
                                navController = navController,
                                viewModel3 = productViewModel,
                                productToEdit = product
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Product not found or still loading...", color = Color.Gray)
                            }
                        }
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied", color = Color.Red)
                        }
                    }
                }


                composable(Routes.OrderProcess.routes) {
                    // ✅ Check access before showing screen
                    if (RoleManager.canAccessRoute(Routes.OrderProcess.routes)) {
                        OrderProcessScreen(navController, productViewModel, recipeViewModel, salesReportViewModel)
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(RoleManager.getDefaultRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Access Denied - Redirecting...", color = Color.Red)
                        }
                    }
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
            .also {
                android.util.Log.d("MainActivity", "✅ Coil ImageLoader configured with network support")
            }
    }

    // 🆕 Schedule automatic expiration check worker
    private fun scheduleExpirationCheckWorker() {
        try {
            android.util.Log.d("MainActivity", "🔄 Scheduling ExpirationCheckWorker...")

            // Create a periodic work request that runs daily at 6 AM
            val expirationCheckWork = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(
                1,  // repeat interval
                TimeUnit.DAYS
            )
                .setInitialDelay(calculateInitialDelay(), java.util.concurrent.TimeUnit.MINUTES)
                .addTag("expiration_check")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15000, // 15 seconds minimum backoff (WorkManager default)
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            // Enqueue the work (KEEP replaces any existing work with same tag)
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "expiration_check_daily",
                ExistingPeriodicWorkPolicy.KEEP,
                expirationCheckWork
            )

            android.util.Log.d("MainActivity", "✅ ExpirationCheckWorker scheduled for daily 6 AM")

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to schedule ExpirationCheckWorker: ${e.message}", e)
        }
    }

    // Calculate minutes until 6 AM for initial delay
    private fun calculateInitialDelay(): Long {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        val targetHour = 6 // 6 AM

        val delayMinutes = if (currentHour < targetHour) {
            // If before 6 AM, wait until 6 AM today
            (targetHour - currentHour) * 60 - currentMinute
        } else {
            // If after 6 AM, wait until 6 AM tomorrow
            ((24 - currentHour) + targetHour) * 60 - currentMinute
        }

        android.util.Log.d("MainActivity", "⏰ ExpirationCheckWorker initial delay: $delayMinutes minutes")
        return delayMinutes.toLong()
    }

    private fun runExpirationCheckNow() {
        val immediateWork = OneTimeWorkRequestBuilder<ExpirationCheckWorker>()
            .addTag("expiration_check_now")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "expiration_check_now",
                ExistingWorkPolicy.REPLACE,
                immediateWork
            )

        android.util.Log.d("MainActivity", "🚀 Expiration check enqueued immediately")
    }

}


