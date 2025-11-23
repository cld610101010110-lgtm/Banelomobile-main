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
    import com.jakewharton.threetenabp.AndroidThreeTen
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            AndroidThreeTen.init(this)
            CloudinaryHelper.initialize(this)
            AuditHelper.initialize(this)
            android.util.Log.d("MainActivity", "✅ AuditHelper initialized")
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

//            migrateFirebaseData()
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
                                Text("Access Denied - Redirecting...", color = Color.Red)
                            }
                        }
                    }
                    composable(Routes.OrderProcess.routes) {
                        // ✅ Check access before showing screen
                        if (RoleManager.canAccessRoute(Routes.OrderProcess.routes)) {
                            OrderProcessScreen(navController, productViewModel, recipeViewModel)
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
/*        private fun migrateFirebaseData() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    android.util.Log.d("Migration", "━━━━━━━━━━━━━━━━━━━━━━━━")
                    android.util.Log.d("Migration", "🔄 Starting Firebase data migration...")

                    val firestore = FirebaseFirestore.getInstance()

                    // ============================================
                    // PART 1: Migrate Products Collection
                    // ============================================
                    android.util.Log.d("Migration", "\n📦 MIGRATING PRODUCTS...")
                    val productsRef = firestore.collection("products")
                    val productsSnapshot = productsRef.get().await()
                    android.util.Log.d("Migration", "📦 Found ${productsSnapshot.documents.size} products to check")

                    var productsUpdated = 0
                    var productsErrors = 0

                    for (doc in productsSnapshot.documents) {
                        try {
                            val docId = doc.id
                            val currentCategory = doc.getString("category") ?: ""
                            val currentImageUri = doc.get("imageUri")

                            var needsUpdate = false
                            val updates = hashMapOf<String, Any>()

                            // ✅ FIX 1: Update category names
                            val newCategory = when (currentCategory.lowercase()) {
                                "hot drinks", "cold drinks" -> {
                                    needsUpdate = true
                                    "Beverages"
                                }
                                "snacks" -> {
                                    needsUpdate = true
                                    "Pastries"
                                }
                                else -> currentCategory
                            }

                            if (needsUpdate) {
                                updates["category"] = newCategory
                                android.util.Log.d("Migration", "  📝 $docId: $currentCategory → $newCategory")
                            }

                            // ✅ FIX 2: Fix imageUri (convert NaN to empty string)
                            if (currentImageUri != null && currentImageUri !is String) {
                                updates["imageUri"] = ""
                                android.util.Log.d("Migration", "  🖼️ $docId: Fixed imageUri (was NaN)")
                                needsUpdate = true
                            } else if (currentImageUri is String && currentImageUri.equals("NaN", ignoreCase = true)) {
                                updates["imageUri"] = ""
                                android.util.Log.d("Migration", "  🖼️ $docId: Fixed imageUri (was 'NaN' string)")
                                needsUpdate = true
                            }

                            // Apply updates if needed
                            if (updates.isNotEmpty()) {
                                productsRef.document(docId).update(updates).await()
                                productsUpdated++
                                android.util.Log.d("Migration", "  ✅ Updated $docId")
                            }

                        } catch (e: Exception) {
                            productsErrors++
                            android.util.Log.e("Migration", "  ❌ Error updating product ${doc.id}: ${e.message}")
                        }
                    }

                    // ============================================
                    // PART 2: Migrate Sales Collection
                    // ============================================
                    android.util.Log.d("Migration", "\n💰 MIGRATING SALES...")
                    val salesRef = firestore.collection("sales")
                    val salesSnapshot = salesRef.get().await()
                    android.util.Log.d("Migration", "💰 Found ${salesSnapshot.documents.size} sales to check")

                    var salesUpdated = 0
                    var salesErrors = 0

                    for (doc in salesSnapshot.documents) {
                        try {
                            val docId = doc.id
                            val currentCategory = doc.getString("category") ?: ""

                            // ✅ Update category names in sales
                            val newCategory = when (currentCategory.lowercase()) {
                                "hot drinks", "cold drinks" -> {
                                    android.util.Log.d("Migration", "  📝 Sale $docId: $currentCategory → Beverages")
                                    "Beverages"
                                }
                                "snacks" -> {
                                    android.util.Log.d("Migration", "  📝 Sale $docId: $currentCategory → Pastries")
                                    "Pastries"
                                }
                                else -> null
                            }

                            // Apply update if category needs to change
                            if (newCategory != null && newCategory != currentCategory) {
                                salesRef.document(docId).update("category", newCategory).await()
                                salesUpdated++
                                android.util.Log.d("Migration", "  ✅ Updated sale $docId")
                            }

                        } catch (e: Exception) {
                            salesErrors++
                            android.util.Log.e("Migration", "  ❌ Error updating sale ${doc.id}: ${e.message}")
                        }
                    }

                    // ============================================
                    // SUMMARY
                    // ============================================
                    android.util.Log.d("Migration", "\n━━━━━━━━━━━━━━━━━━━━━━━━")
                    android.util.Log.d("Migration", "✅ MIGRATION COMPLETE!")
                    android.util.Log.d("Migration", "")
                    android.util.Log.d("Migration", "📦 Products:")
                    android.util.Log.d("Migration", "   Updated: $productsUpdated documents")
                    android.util.Log.d("Migration", "   Errors: $productsErrors documents")
                    android.util.Log.d("Migration", "")
                    android.util.Log.d("Migration", "💰 Sales:")
                    android.util.Log.d("Migration", "   Updated: $salesUpdated documents")
                    android.util.Log.d("Migration", "   Errors: $salesErrors documents")
                    android.util.Log.d("Migration", "━━━━━━━━━━━━━━━━━━━━━━━━")

                } catch (e: Exception) {
                    android.util.Log.e("Migration", "❌ Migration failed: ${e.message}", e)
                }
            }
        }

 */
    }


