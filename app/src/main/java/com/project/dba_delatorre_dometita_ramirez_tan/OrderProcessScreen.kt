package com.project.dba_delatorre_dometita_ramirez_tan

import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


data class ReceiptData(
    val items: List<Pair<String, Int>>, // Product name to quantity
    val subtotal: Double,               // Original price before VAT/discount
    val vatAmount: Double,              // 12% VAT
    val discountType: String,           // "None", "Senior Citizen", "PWD", "Others"
    val discountPercent: Double,        // Discount percentage applied
    val discountAmount: Double,         // Amount discounted
    val totalPrice: Double,             // Final price after VAT and discount
    val cashReceived: Double,
    val change: Double,
    val paymentMode: String,
    val gcashReferenceId: String,
    val orderDate: String
)

// Philippine discount types
object DiscountTypes {
    const val NONE = "None"
    const val SENIOR_CITIZEN = "Senior Citizen (20%)"
    const val PWD = "PWD (20%)"
    const val OTHERS = "Others"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderProcessScreen(
    navController: NavController,
    viewModel3: ProductViewModel,
    recipeViewModel: RecipeViewModel,
    salesReportViewModel: SalesReportViewModel
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var cartVisible by remember { mutableStateOf(false) }
    var cartItems by remember { mutableStateOf<List<Entity_Products>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val searchQuery = remember { mutableStateOf("") }
    var cashReceived by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }
    var paymentMode by remember { mutableStateOf("Cash") } // "Cash" or "GCash"
    var gcashReferenceId by remember { mutableStateOf("") }
    var showQuantityDialog by remember { mutableStateOf(false) }
    var quantityDialogProduct by remember { mutableStateOf<Entity_Products?>(null) }
    var quantityInput by remember { mutableStateOf("") }
    var showPrintableReceipt by remember { mutableStateOf(false) }
    var receiptData by remember { mutableStateOf<ReceiptData?>(null) }

    // Discount states
    var selectedDiscount by remember { mutableStateOf(DiscountTypes.NONE) }
    var customDiscountPercent by remember { mutableStateOf("") }
    var showCustomDiscountDialog by remember { mutableStateOf(false) }
    var discountDropdownExpanded by remember { mutableStateOf(false) }

    // Philippine VAT rate
    val VAT_RATE = 0.12

    // Calculate prices
    val subtotal = cartItems.sumOf { it.price }
    val vatAmount = subtotal * VAT_RATE

    // Calculate discount
    val discountPercent = when (selectedDiscount) {
        DiscountTypes.SENIOR_CITIZEN -> 20.0
        DiscountTypes.PWD -> 20.0
        DiscountTypes.OTHERS -> customDiscountPercent.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    // For Senior/PWD: They are VAT exempt, so discount is applied on VAT-exclusive price
    // For Others: Discount is applied on the subtotal (VAT inclusive for simplicity)
    val discountAmount = if (selectedDiscount == DiscountTypes.SENIOR_CITIZEN || selectedDiscount == DiscountTypes.PWD) {
        // VAT exempt + 20% discount on VAT-exclusive price
        val vatExclusivePrice = subtotal / (1 + VAT_RATE)
        vatExclusivePrice * (discountPercent / 100)
    } else {
        subtotal * (discountPercent / 100)
    }

    // Final total calculation
    val totalPrice = if (selectedDiscount == DiscountTypes.SENIOR_CITIZEN || selectedDiscount == DiscountTypes.PWD) {
        // VAT exempt: use VAT-exclusive price minus discount
        val vatExclusivePrice = subtotal / (1 + VAT_RATE)
        vatExclusivePrice - discountAmount
    } else {
        // Regular: subtotal minus discount (VAT already included in prices)
        subtotal - discountAmount
    }
    val gradient = Brush.verticalGradient(listOf(Color(0xFFF3D3BD), Color(0xFF837060)))

    // ✅ FIX: Store available quantities in state (outside of map)
    var availableQuantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // ✅ FIX: Force FULL data refresh when screen loads - fixes inconsistent availability
    LaunchedEffect(Unit) {
        android.util.Log.d("OrderProcess", "🔄 Force refreshing data on screen entry...")
        viewModel3.getAllProducts() // Refresh from API
        recipeViewModel.syncRecipes() // Refresh recipes (correct method name)
        kotlinx.coroutines.delay(600) // Wait for data
        refreshTrigger++ // Trigger recalculation
        android.util.Log.d("OrderProcess", "✅ Force refresh complete")
    }

    // ✅ Calculate available quantities based on INVENTORY B for recipe-based products
    LaunchedEffect(viewModel3.productList, refreshTrigger) {
        val quantities = mutableMapOf<String, Int>()

        // ✅ Calculate for recipe-based products (Beverages and Pastries) using INVENTORY B ONLY
        viewModel3.productList
            .filter {
                it.category.equals("Beverages", ignoreCase = true) ||
                it.category.equals("Pastries", ignoreCase = true) ||
                !it.category.equals("Ingredients", ignoreCase = true) ||
                !it.category.equals("Ingredient", ignoreCase = true)
            }
            .forEach { product ->
                try {
                    // ✅ KEY CHANGE: Use getAvailableQuantityFromInventoryB instead of getAvailableQuantity
                    val maxServings = recipeViewModel.getAvailableQuantityFromInventoryB(product.firebaseId)
                    quantities[product.firebaseId] = maxServings
                    android.util.Log.d("OrderProcess", "🧮 ${product.name} (${product.category}): $maxServings servings available (Inventory B)")
                } catch (e: Exception) {
                    android.util.Log.e("OrderProcess", "❌ Error calculating servings for ${product.name}: ${e.message}")
                    quantities[product.firebaseId] = 0
                }
            }

        // For non-recipe products (e.g., Snacks), use inventoryB quantity
        viewModel3.productList
            .filter {
                !it.category.equals("Ingredients", ignoreCase = true) &&
                !it.category.equals("Beverages", ignoreCase = true) &&
                !it.category.equals("Pastries", ignoreCase = true)
            }
            .forEach { product ->
                // ✅ KEY CHANGE: Use inventoryB instead of total quantity
                quantities[product.firebaseId] = product.inventoryB
                android.util.Log.d("OrderProcess", "📦 ${product.name} (${product.category}): Inventory B = ${product.inventoryB}")
            }

        availableQuantities = quantities
    }

    // ✅ Map products with calculated quantities
    val products = viewModel3.productList
        .filter { !it.category.equals("Ingredients", ignoreCase = true) }
        .filter {
            it.name.contains(searchQuery.value, ignoreCase = true) ||
                    it.category.contains(searchQuery.value, ignoreCase = true)
        }
        .map { product ->
            product.copy(quantity = availableQuantities[product.firebaseId] ?: product.quantity)
        }
        .sortedWith(
            compareByDescending<Entity_Products> { it.quantity > 0 }  // Available first
                .thenBy { it.name }                                     // Then A-Z within each group
        )

    LaunchedEffect(cartVisible, refreshTrigger) {
        if (!cartVisible) {
            kotlinx.coroutines.delay(300)
            viewModel3.getAllProducts()
        }
    }

    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        SidebarDrawer(navController)
    }) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF5D4037)
                ) {
                    TopAppBar(
                        title = {
                            Text("Order Process", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { cartVisible = !cartVisible }) {
                                BadgedBox(
                                    badge = {
                                        if (cartItems.isNotEmpty()) {
                                            Badge { Text("${cartItems.size}") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradient)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!cartVisible) {
                        OutlinedTextField(
                            value = searchQuery.value,
                            onValueChange = { searchQuery.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Search products...") },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                            val categories = viewModel3.productList
                                .map { it.category }
                                .distinct()
                                .filter { !it.equals("Ingredients", ignoreCase = true) }

                            val allCategories = listOf("All") + categories

                            items(allCategories.size) { i ->
                                val category = allCategories[i]

                                val chipColor = when (category) {
                                    "All" -> Color(0xFF6F4E37)
                                    "Pastries" -> Color(0xFF6F4E37)
                                    "Beverages" -> Color(0xFF6F4E37)
                                    else -> Color(0xFFA88164)
                                }

                                val isSelected = if (category == "All") {
                                    searchQuery.value.isEmpty()
                                } else {
                                    searchQuery.value == category
                                }

                                AssistChip(
                                    onClick = {
                                        searchQuery.value = if (category == "All") "" else category
                                    },
                                    label = { Text(category, color = Color.White) },
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSelected) chipColor.copy(alpha = 1f) else chipColor.copy(alpha = 0.6f)
                                    ),
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                                    } else null
                                )
                            }
                        }

                        products.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowItems.forEach { product ->
                                    // ✅ Check if product is out of stock
                                    val isOutOfStock = product.quantity <= 0

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(200.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            // ✅ Gray out if out of stock
                                            containerColor = if (isOutOfStock) Color(0xFFE0E0E0) else Color.White
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // ✅ Add overlay for out of stock items
                                            Box {
                                                if (!product.imageUri.isNullOrEmpty()) {
                                                    // ✅ Load image from Cloudinary URL (Coil handles URLs directly)
                                                    Image(
                                                        painter = rememberAsyncImagePainter(
                                                            model = product.imageUri,
                                                            error = painterResource(R.drawable.ic_launcher_foreground),
                                                            placeholder = painterResource(R.drawable.ic_launcher_foreground)
                                                        ),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .height(90.dp)
                                                            .fillMaxWidth(),
                                                        contentScale = ContentScale.Crop,
                                                        // ✅ Dim image if out of stock
                                                        alpha = if (isOutOfStock) 0.4f else 1f
                                                    )
                                                } else {
                                                    // Show placeholder when no image
                                                    Box(
                                                        modifier = Modifier
                                                            .height(90.dp)
                                                            .fillMaxWidth()
                                                            .background(Color.LightGray, RoundedCornerShape(8.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("No Image", fontSize = 12.sp, color = Color.DarkGray)
                                                    }
                                                }

                                                // ✅ Show "OUT OF STOCK" badge
                                                if (isOutOfStock) {
                                                    Box(
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .background(Color.Black.copy(alpha = 0.5f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "OUT OF STOCK",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                product.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                // ✅ Dim text if out of stock
                                                color = if (isOutOfStock) Color.Gray else Color.Black
                                            )
                                            Text(
                                                "₱${product.price}",
                                                fontSize = 12.sp,
                                                color = if (isOutOfStock) Color.Gray else Color.Black
                                            )
                                            Text(
                                                "Available: ${product.quantity}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (product.quantity > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // ✅ Disable button if out of stock
                                            Button(
                                                onClick = {
                                                    if (!isOutOfStock) {
                                                        cartItems = cartItems + product
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("${product.name} added to cart")
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(0.9f),
                                                enabled = !isOutOfStock, // ✅ KEY CHANGE: Disable when out of stock
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isOutOfStock) Color.Gray else Color(0xFFA88164),
                                                    disabledContainerColor = Color.Gray
                                                )
                                            ) {
                                                Text(
                                                    if (isOutOfStock) "Unavailable" else "Add",
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // ✅ Cart view (unchanged)
                        Text("Order Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                        cartItems.groupBy { it.firebaseId }.forEach { (_, items) ->
                            val product = items.first()
                            val quantity = items.size

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(product.name, fontWeight = FontWeight.Bold)
                                        Text("₱${product.price} x $quantity = ₱${product.price.toInt() * quantity}")
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            cartItems = cartItems.toMutableList().apply { remove(product) }
                                        }) {
                                            Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }

                                        // Clickable quantity
                                        Text(
                                            text = "$quantity",
                                            modifier = Modifier
                                                .clickable {
                                                    quantityDialogProduct = product
                                                    quantityInput = quantity.toString()
                                                    showQuantityDialog = true
                                                }
                                                .background(Color(0xFFF3D3BD), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )

                                        IconButton(onClick = {
                                            cartItems = cartItems + product
                                        }) {
                                            Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Discount Selection
                        Text("Discount:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = discountDropdownExpanded,
                            onExpandedChange = { discountDropdownExpanded = !discountDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = if (selectedDiscount == DiscountTypes.OTHERS && customDiscountPercent.isNotEmpty())
                                    "Others ($customDiscountPercent%)" else selectedDiscount,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = discountDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = discountDropdownExpanded,
                                onDismissRequest = { discountDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(DiscountTypes.NONE) },
                                    onClick = {
                                        selectedDiscount = DiscountTypes.NONE
                                        customDiscountPercent = ""
                                        discountDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(DiscountTypes.SENIOR_CITIZEN) },
                                    onClick = {
                                        selectedDiscount = DiscountTypes.SENIOR_CITIZEN
                                        customDiscountPercent = ""
                                        discountDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(DiscountTypes.PWD) },
                                    onClick = {
                                        selectedDiscount = DiscountTypes.PWD
                                        customDiscountPercent = ""
                                        discountDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Others (Custom %)") },
                                    onClick = {
                                        selectedDiscount = DiscountTypes.OTHERS
                                        discountDropdownExpanded = false
                                        showCustomDiscountDialog = true
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Price breakdown
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal:", color = Color.Gray)
                                    Text("₱${"%.2f".format(subtotal)}")
                                }

                                if (selectedDiscount == DiscountTypes.SENIOR_CITIZEN || selectedDiscount == DiscountTypes.PWD) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("VAT (12%):", color = Color.Gray)
                                        Text("₱${"%.2f".format(vatAmount)} (Exempt)", color = Color.Green)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("VAT (12% included):", color = Color.Gray)
                                        Text("₱${"%.2f".format(vatAmount)}")
                                    }
                                }

                                if (discountAmount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Discount (${"%.0f".format(discountPercent)}%):", color = Color.Green)
                                        Text("-₱${"%.2f".format(discountAmount)}", color = Color.Green)
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("₱${"%.2f".format(totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Payment Mode Selection
                        Text("Payment Mode:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    paymentMode = "Cash"
                                    gcashReferenceId = ""
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (paymentMode == "Cash") Color(0xFF5D4037) else Color(0xFFA88164)
                                )
                            ) {
                                Text("Cash", color = Color.White)
                            }
                            Button(
                                onClick = { paymentMode = "GCash" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (paymentMode == "GCash") Color(0xFF5D4037) else Color(0xFFA88164)
                                )
                            ) {
                                Text("GCash", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (paymentMode == "Cash") {
                            OutlinedTextField(
                                value = cashReceived,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        cashReceived = it
                                    }
                                },
                                label = { Text("Enter cash received") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            OutlinedTextField(
                                value = gcashReferenceId,
                                onValueChange = { newValue ->
                                    // Only accept digits and limit to 13 characters
                                    if (newValue.all { it.isDigit() } && newValue.length <= 13) {
                                        gcashReferenceId = newValue
                                    }
                                },
                                label = { Text("Enter GCash Reference ID (13 digits)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                supportingText = {
                                    Text(
                                        text = "${gcashReferenceId.length}/13 digits",
                                        color = if (gcashReferenceId.length == 13) Color.Green else Color.Gray
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                // Fix: Check if cart is empty
                                if (cartItems.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Cart is empty!")
                                    }
                                    return@Button
                                }

                                if (paymentMode == "Cash") {
                                    val cashAmount = cashReceived.toDoubleOrNull() ?: 0.0
                                    val change = cashAmount - totalPrice

                                    if (change < 0) {
                                        val shortage = totalPrice - cashAmount
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Insufficient cash! Need ₱${String.format("%.2f", shortage)} more.")
                                        }
                                        return@Button
                                    }
                                } else { // GCash
                                    if (gcashReferenceId.isBlank()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Please enter GCash Reference ID.")
                                        }
                                        return@Button
                                    }
                                    if (gcashReferenceId.length != 13) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("GCash Reference ID must be exactly 13 digits.")
                                        }
                                        return@Button
                                    }
                                }

                                // Process the order directly
                                val cashAmount = if (paymentMode == "Cash") cashReceived.toDoubleOrNull() ?: 0.0 else totalPrice.toDouble()
                                val change = if (paymentMode == "Cash") cashAmount - totalPrice else 0.0
                                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                                // Prepare receipt data
                                val items = cartItems.groupBy { it.firebaseId }.map { (_, items) ->
                                    val product = items.first()
                                    val quantity = items.size
                                    "${product.name} (₱${product.price.toInt()})" to quantity
                                }
                                receiptData = ReceiptData(
                                    items = items,
                                    subtotal = subtotal,
                                    vatAmount = vatAmount,
                                    discountType = selectedDiscount,
                                    discountPercent = discountPercent,
                                    discountAmount = discountAmount,
                                    totalPrice = totalPrice,
                                    cashReceived = cashAmount,
                                    change = if (change >= 0) change else 0.0,
                                    paymentMode = paymentMode,
                                    gcashReferenceId = if (paymentMode == "GCash") gcashReferenceId else "",
                                    orderDate = currentDate
                                )

                                // FULL API PROCESSING — per product in cart
                                cartItems.groupBy { it.firebaseId }.forEach { (_, items) ->

                                    val product = items.first()
                                    val quantity = items.size

                                    viewModel3.processSale(
                                        product = product,
                                        quantity = quantity,
                                        paymentMode = paymentMode,
                                        gcashReferenceId = if (paymentMode == "GCash") gcashReferenceId else null,
                                        cashierUsername = "admin" // or use your logged-in username
                                    ) { success ->
                                        if (success) {
                                            // ✅ FIX: Log sale to audit trail
                                            val saleTotal = product.price * quantity
                                            AuditHelper.logSale(product.name, quantity, saleTotal)

                                            // ✅ FIX: Save sale to local Room database for Overview page
                                            val saleEntity = Entity_SalesReport(
                                                productName = product.name,
                                                category = product.category,
                                                quantity = quantity,
                                                price = product.price,
                                                orderDate = currentDate,
                                                productFirebaseId = product.firebaseId,
                                                paymentMode = paymentMode,
                                                gcashReferenceId = if (paymentMode == "GCash") gcashReferenceId else ""
                                            )
                                            salesReportViewModel.insertSale(saleEntity)

                                            android.util.Log.d("OrderProcess", "✅ Sale saved to Room database for Overview")
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Sale failed for ${product.name} - Check ingredient stock!",
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }
                                    }
                                }


                                // ✅ Show success feedback
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Transaction completed successfully! ✅",
                                        duration = SnackbarDuration.Short
                                    )
                                }

                                cartItems = emptyList()
                                cashReceived = ""
                                gcashReferenceId = ""
                                paymentMode = "Cash"
                                selectedDiscount = DiscountTypes.NONE
                                customDiscountPercent = ""
                                cartVisible = false
                                showPrintableReceipt = true

                                // ✅ Refresh to update available quantities
                                refreshTrigger++
                                viewModel3.getAllProducts()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                        ) {
                            Text("Complete", color = Color.White)
                        }
                    }
                }
            }
        )

        // Quantity Input Dialog
        if (showQuantityDialog && quantityDialogProduct != null) {
            AlertDialog(
                onDismissRequest = { showQuantityDialog = false },
                title = { Text("Enter Quantity") },
                text = {
                    Column {
                        Text("Product: ${quantityDialogProduct?.name}")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = {
                                // Only allow digits
                                if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                    quantityInput = it
                                }
                            },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newQuantity = quantityInput.toIntOrNull() ?: 0
                        if (newQuantity > 0 && quantityDialogProduct != null) {
                            val product = quantityDialogProduct!!
                            // Remove all instances of this product
                            cartItems = cartItems.filter { it.firebaseId != product.firebaseId }
                            // Add the new quantity
                            repeat(newQuantity) {
                                cartItems = cartItems + product
                            }
                        }
                        showQuantityDialog = false
                        quantityDialogProduct = null
                        quantityInput = ""
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showQuantityDialog = false
                        quantityDialogProduct = null
                        quantityInput = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Custom Discount Dialog
        if (showCustomDiscountDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCustomDiscountDialog = false
                    if (customDiscountPercent.isEmpty()) {
                        selectedDiscount = DiscountTypes.NONE
                    }
                },
                title = { Text("Enter Custom Discount") },
                text = {
                    Column {
                        Text("Enter discount percentage (numbers only):")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customDiscountPercent,
                            onValueChange = {
                                // Only allow digits and one decimal point, max 100
                                if (it.isEmpty() || (it.matches(Regex("^\\d*\\.?\\d*$")) && (it.toDoubleOrNull() ?: 0.0) <= 100)) {
                                    customDiscountPercent = it
                                }
                            },
                            label = { Text("Discount %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            suffix = { Text("%") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val percent = customDiscountPercent.toDoubleOrNull() ?: 0.0
                        if (percent > 0 && percent <= 100) {
                            showCustomDiscountDialog = false
                        } else {
                            customDiscountPercent = ""
                            selectedDiscount = DiscountTypes.NONE
                            showCustomDiscountDialog = false
                        }
                    }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        customDiscountPercent = ""
                        selectedDiscount = DiscountTypes.NONE
                        showCustomDiscountDialog = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Printable Receipt Dialog
        if (showPrintableReceipt && receiptData != null) {
            PrintableReceiptDialog(
                receiptData = receiptData!!,
                onDismiss = {
                    showPrintableReceipt = false
                    receiptData = null
                },
                onPrint = { receipt ->
                    printReceipt(context, receipt)
                }
            )
        }
    }
}

@Composable
fun PrintableReceiptDialog(
    receiptData: ReceiptData,
    onDismiss: () -> Unit,
    onPrint: (ReceiptData) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Receipt Header
                Text(
                    text = "RECEIPT",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Mobile POS System",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = receiptData.orderDate,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Gray
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Items
                receiptData.items.forEach { (productInfo, quantity) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = productInfo,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "x$quantity",
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Subtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Subtotal:", fontSize = 14.sp)
                    Text(text = "₱${"%.2f".format(receiptData.subtotal)}", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))

                // VAT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "VAT (12%):", fontSize = 14.sp)
                    if (receiptData.discountType == DiscountTypes.SENIOR_CITIZEN ||
                        receiptData.discountType == DiscountTypes.PWD) {
                        Text(text = "₱${"%.2f".format(receiptData.vatAmount)} (Exempt)", fontSize = 14.sp, color = Color.Green)
                    } else {
                        Text(text = "₱${"%.2f".format(receiptData.vatAmount)} (Included)", fontSize = 14.sp)
                    }
                }

                // Discount (if applied)
                if (receiptData.discountAmount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Discount (${receiptData.discountType} - ${"%.0f".format(receiptData.discountPercent)}%):",
                            fontSize = 14.sp,
                            color = Color.Green
                        )
                        Text(
                            text = "-₱${"%.2f".format(receiptData.discountAmount)}",
                            fontSize = 14.sp,
                            color = Color.Green
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₱${"%.2f".format(receiptData.totalPrice)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Details
                Text(
                    text = "Payment Method: ${receiptData.paymentMode}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (receiptData.paymentMode == "Cash") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Cash Received:", fontSize = 14.sp)
                        Text(
                            text = "₱${String.format("%.2f", receiptData.cashReceived)}",
                            fontSize = 14.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Change:", fontSize = 14.sp)
                        Text(
                            text = "₱${String.format("%.2f", receiptData.change)}",
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "GCash Reference ID: ${receiptData.gcashReferenceId}",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Thank you for your purchase!",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPrint(receiptData) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                    ) {
                        Text("Print", color = Color.White)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA88164))
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

fun printReceipt(context: android.content.Context, receiptData: ReceiptData) {
    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager

    // Create HTML content for the receipt
    val htmlContent = """
        <html>
        <head>
            <style>
                body {
                    font-family: monospace;
                    padding: 20px;
                    max-width: 300px;
                    margin: 0 auto;
                }
                h1 {
                    text-align: center;
                    font-size: 24px;
                    margin-bottom: 5px;
                }
                .header {
                    text-align: center;
                    margin-bottom: 20px;
                }
                .date {
                    text-align: center;
                    color: #666;
                    font-size: 12px;
                }
                .divider {
                    border-top: 1px dashed #000;
                    margin: 15px 0;
                }
                .item-row {
                    display: flex;
                    justify-content: space-between;
                    margin: 5px 0;
                }
                .total-row {
                    display: flex;
                    justify-content: space-between;
                    font-weight: bold;
                    font-size: 18px;
                    margin-top: 10px;
                }
                .payment-info {
                    margin-top: 15px;
                }
                .footer {
                    text-align: center;
                    margin-top: 20px;
                    font-style: italic;
                }
            </style>
        </head>
        <body>
            <h1>RECEIPT</h1>
            <div class="header">
                <div>Mobile POS System</div>
            </div>
            <div class="date">${receiptData.orderDate}</div>
            <div class="divider"></div>
            ${receiptData.items.joinToString("") { (productInfo, quantity) ->
                "<div class=\"item-row\"><span>$productInfo</span><span>x$quantity</span></div>"
            }}
            <div class="divider"></div>
            <div class="item-row">
                <span>Subtotal:</span>
                <span>₱${String.format("%.2f", receiptData.subtotal)}</span>
            </div>
            <div class="item-row">
                <span>VAT (12%):</span>
                <span>₱${String.format("%.2f", receiptData.vatAmount)}${if (receiptData.discountType == DiscountTypes.SENIOR_CITIZEN || receiptData.discountType == DiscountTypes.PWD) " (Exempt)" else " (Incl.)"}</span>
            </div>
            ${if (receiptData.discountAmount > 0) """
            <div class="item-row" style="color: green;">
                <span>Discount (${receiptData.discountType}):</span>
                <span>-₱${String.format("%.2f", receiptData.discountAmount)}</span>
            </div>
            """ else ""}
            <div class="divider"></div>
            <div class="total-row">
                <span>TOTAL:</span>
                <span>₱${String.format("%.2f", receiptData.totalPrice)}</span>
            </div>
            <div class="payment-info">
                <div><strong>Payment Method:</strong> ${receiptData.paymentMode}</div>
                ${if (receiptData.paymentMode == "Cash") """
                    <div>Cash Received: ₱${String.format("%.2f", receiptData.cashReceived)}</div>
                    <div>Change: ₱${String.format("%.2f", receiptData.change)}</div>
                """ else """
                    <div>GCash Reference ID: ${receiptData.gcashReferenceId}</div>
                """}
            </div>
            <div class="divider"></div>
            <div class="footer">Thank you for your purchase!</div>
        </body>
        </html>
    """.trimIndent()

    // Create a WebView to render the HTML
    val webView = WebView(context)
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            // Create a print adapter
            val printAdapter = webView.createPrintDocumentAdapter("Receipt")

            // Create a print job
            val jobName = "Receipt_${System.currentTimeMillis()}"
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
}