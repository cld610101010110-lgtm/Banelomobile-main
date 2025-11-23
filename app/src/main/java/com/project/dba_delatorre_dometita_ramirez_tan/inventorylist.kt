package com.project.dba_delatorre_dometita_ramirez_tan

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    navController: NavController,
    viewModel3: ProductViewModel,
    recipeViewModel: RecipeViewModel  // ✅ ADD THIS PARAMETER
) {
    // ✅ State to store max servings for recipe-based products
    var maxServingsMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // ✅ Fetch products when screen opens
    LaunchedEffect(Unit) {
        android.util.Log.d("InventoryList", "🔄 Fetching products from Firebase...")
        viewModel3.getAllProducts()

        android.util.Log.d("InventoryList", "📊 Products in UI:")
        viewModel3.productList.forEach { product ->
            android.util.Log.d("InventoryList", "  • ${product.firebaseId}: ${product.name} (Qty: ${product.quantity})")
        }
    }

    // ✅ Calculate max servings for beverages and pastries
    LaunchedEffect(viewModel3.productList) {
        val newMaxServingsMap = mutableMapOf<String, Int>()

        viewModel3.productList
            .filter {
                it.category.equals("Beverages", ignoreCase = true) ||
                it.category.equals("Pastries", ignoreCase = true)
            }
            .forEach { product ->
                try {
                    val maxServings = recipeViewModel.getAvailableQuantity(product.firebaseId)
                    newMaxServingsMap[product.firebaseId] = maxServings
                    android.util.Log.d("InventoryList", "📊 ${product.name}: $maxServings servings available")
                } catch (e: Exception) {
                    android.util.Log.e("InventoryList", "❌ Error calculating servings for ${product.name}: ${e.message}")
                    newMaxServingsMap[product.firebaseId] = 0
                }
            }

        maxServingsMap = newMaxServingsMap
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isLoading = viewModel3.isLoading
    val errorMessage = viewModel3.errorMessage

    var productToDelete by remember { mutableStateOf<Entity_Products?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var setupStatus by remember { mutableStateOf("") }
    var isSettingUp by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    val chipOptions = listOf("All", "Beverages", "Pastries", "Ingredients", "Snacks")
    var selectedOption by remember { mutableStateOf("All") }

    val selectedChipColor = Color(0xFF6F4E37)
    val unselectedChipColor = Color(0xFFEEE0CB)
    val selectedTextColor = Color.White
    val unselectedTextColor = Color.Black

    val filteredProducts = viewModel3.productList
        .filter {
            it.name.contains(searchText.text, ignoreCase = true) &&
                    (selectedOption == "All" || it.category.equals(selectedOption, ignoreCase = true))
        }
        .sortedWith(
            compareByDescending<Entity_Products> {
                // ✅ Use max servings for beverages/pastries, quantity for ingredients
                when {
                    it.category.equals("Beverages", ignoreCase = true) ||
                    it.category.equals("Pastries", ignoreCase = true) -> {
                        (maxServingsMap[it.firebaseId] ?: 0) > 0
                    }
                    else -> it.quantity > 0
                }
            }  // Available first
                .thenBy { it.name }  // Then A-Z within each group
        )

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { SidebarDrawer(navController) }
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("AddProductScreen") },
                    containerColor = Color(0xFFA26153),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            },
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF5D4037),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Inventory List",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        actions = {
                            // Setup/Tools Button
                            IconButton(onClick = {
                                showSetupDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = "Setup Tools",
                                    tint = Color.White
                                )
                            }
                            // Cost Analysis Button
                            IconButton(onClick = {
                                navController.navigate(Routes.R_IngredientCostView.routes)
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "View Costs",
                                    tint = Color.White
                                )
                            }
                            // Transfer Button
                            IconButton(onClick = {
                                navController.navigate(Routes.R_InventoryTransfer.routes)
                            }) {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Transfer",
                                    tint = Color.White
                                )
                            }
                            // Waste Button
                            IconButton(onClick = {
                                navController.navigate(Routes.R_WasteMarking.routes)
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Mark Waste",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            content = { paddingValues ->
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = gradient)
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading products...", color = Color.White, fontSize = 16.sp)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = gradient)
                            .padding(paddingValues)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(16.dp),
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }

                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Search product...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.Black,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            chipOptions.forEach { option ->
                                FilterChip(
                                    selected = selectedOption == option,
                                    onClick = { selectedOption = option },
                                    label = {
                                        Text(
                                            option,
                                            color = if (selectedOption == option) selectedTextColor else unselectedTextColor
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 1.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = unselectedChipColor,
                                        selectedContainerColor = selectedChipColor
                                    ),
                                    border = null
                                )
                            }
                        }

                        if (filteredProducts.isEmpty()) {
                            Text(
                                text = if (viewModel3.productList.isEmpty()) "No products available. Add products in Firebase!" else "No products match your search.",
                                fontSize = 18.sp,
                                color = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            filteredProducts.forEach { product ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        if (product.imageUri.isNotEmpty()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    model = product.imageUri,
                                                    error = painterResource(R.drawable.ic_launcher_foreground),
                                                    placeholder = painterResource(R.drawable.ic_launcher_foreground)
                                                ),
                                                contentDescription = "Product Image",
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.LightGray),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("No Image", fontSize = 12.sp, color = Color.DarkGray)
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 12.dp)
                                        ) {
                                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text(product.category, fontSize = 14.sp, color = Color(0xFF4E342E))
                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Dual Inventory Display
                                            Column {
                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    // ✅ Show available quantity based on category
                                                    Text(
                                                        when {
                                                            product.category.equals("Beverages", ignoreCase = true) ||
                                                            product.category.equals("Pastries", ignoreCase = true) -> {
                                                                // Use calculated max servings from recipe
                                                                val maxServings = maxServingsMap[product.firebaseId] ?: 0
                                                                "Available: $maxServings servings"
                                                            }
                                                            else -> "${product.quantity} pcs"
                                                        },
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text("₱${product.price}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // ✅ Show Inventory A and B breakdown ONLY for Ingredients
                                                // For Beverages/Pastries, show that it's recipe-based
                                                if (product.category.equals("Ingredients", ignoreCase = true)) {
                                                    Row(horizontalArrangement = Arrangement.Start) {
                                                        Text(
                                                            "Inv A: ${product.inventoryA}",
                                                            fontSize = 12.sp,
                                                            color = Color.Gray
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            "Inv B: ${product.inventoryB}",
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF6F4E37)
                                                        )
                                                    }

                                                    // ✅ Show cost per unit for ingredients
                                                    if (product.costPerUnit > 0) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            "Cost/Unit: ₱${String.format("%.2f", product.costPerUnit)}",
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF2E7D32),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                } else {
                                                    // For Beverages/Pastries - show that it's recipe-based
                                                    Text(
                                                        "📊 Calculated from ingredient stock",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF8B4513)
                                                    )
                                                }
                                            }
                                            }

                                            // ✅ Out of stock indicator - use max servings for recipes, quantity for ingredients
                                            val isOutOfStock = when {
                                                product.category.equals("Beverages", ignoreCase = true) ||
                                                product.category.equals("Pastries", ignoreCase = true) -> {
                                                    (maxServingsMap[product.firebaseId] ?: 0) == 0
                                                }
                                                else -> product.quantity == 0
                                            }

                                            if (isOutOfStock) {
                                                Text(
                                                    "OUT OF STOCK",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    modifier = Modifier
                                                        .background(Color.Red, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            IconButton(onClick = {
                                                android.util.Log.d("InventoryList", "🖊️ Editing product: ${product.name}")
                                                android.util.Log.d("InventoryList", "Firebase ID: ${product.firebaseId}")
                                                navController.navigate("EditProductScreen/${product.firebaseId}")
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF6D4C41))
                                            }
                                            IconButton(onClick = {
                                                productToDelete = product
                                                showDeleteDialog = true
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showDeleteDialog && productToDelete != null) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Confirm Delete", fontFamily = FontFamily.Serif) },
                                text = { Text("Are you sure you want to delete this record?", fontFamily = FontFamily.Serif) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            AuditHelper.logProductDelete(productToDelete!!.name)
                                            android.util.Log.d("InventoryList", "✅ Audit trail logged for product delete")
                                            viewModel3.deleteProduct(productToDelete!!)
                                            showDeleteDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723))
                                    ) {
                                        Text("Yes", color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { showDeleteDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548))
                                    ) {
                                        Text("Cancel", color = Color.White)
                                    }
                                }
                            )
                        }

                        // Setup Tools Dialog
                        if (showSetupDialog) {
                            AlertDialog(
                                onDismissRequest = { if (!isSettingUp) showSetupDialog = false },
                                title = { Text("Database Cleanup & Setup", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "⚠️ Run this to fix database issues and add recipes!",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "This will:",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("• Remove unused fields from recipes", fontSize = 13.sp)
                                        Text("• Fix missing ingredient Firebase IDs", fontSize = 13.sp)
                                        Text("• Transfer stock values to quantity", fontSize = 13.sp)
                                        Text("• Set realistic cost per unit values", fontSize = 13.sp)
                                        Text("• Add recipes for all pastries", fontSize = 13.sp)
                                        Text("• Enable cost calculations", fontSize = 13.sp)

                                        if (setupStatus.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (setupStatus.contains("✅")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                                )
                                            ) {
                                                Text(
                                                    setupStatus,
                                                    modifier = Modifier.padding(12.dp),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isSettingUp = true
                                                setupStatus = "🔄 Running setup..."

                                                try {
                                                    val result = FirestoreSetup.runCompleteSetup()
                                                    setupStatus = if (result.isSuccess) {
                                                        "✅ ${result.getOrNull()}"
                                                    } else {
                                                        "❌ Error: ${result.exceptionOrNull()?.message}"
                                                    }
                                                } catch (e: Exception) {
                                                    setupStatus = "❌ Error: ${e.message}"
                                                }

                                                isSettingUp = false
                                            }
                                        },
                                        enabled = !isSettingUp,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F4E37))
                                    ) {
                                        if (isSettingUp) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Run Setup", color = Color.White)
                                        }
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = {
                                            showSetupDialog = false
                                            setupStatus = ""
                                        },
                                        enabled = !isSettingUp,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548))
                                    ) {
                                        Text("Close", color = Color.White)
                                    }
                                }
                            )
                        }
                    }
                }

        )
    }
}