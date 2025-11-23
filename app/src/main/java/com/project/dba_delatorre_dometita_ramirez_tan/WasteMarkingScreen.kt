package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteMarkingScreen(
    navController: NavController,
    productViewModel: ProductViewModel,
    wasteLogViewModel: WasteLogViewModel
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    var showSuccessDialog by remember { mutableStateOf(false) }
    var wasteSuccessMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        productViewModel.getAllProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Items as Waste", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8B4513)
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(gradient)
                .padding(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFE4B5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Mark Expired/Damaged Items",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Select items from Inventory B (display stock)\n• Records waste for audit trail\n• Automatically deducts from inventory",
                        fontSize = 13.sp,
                        color = Color(0xFF4E342E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Products with Inventory B stock
            if (productViewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B4513))
                }
            } else {
                val productsWithInventoryB = productViewModel.productList.filter { it.inventoryB > 0 }

                if (productsWithInventoryB.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No items in Inventory B",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B4513)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Transfer items from A → B first",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(productsWithInventoryB) { product ->
                            WasteProductCard(
                                product = product,
                                productViewModel = productViewModel,
                                onMarkAsWaste = { quantity, reason ->
                                    // Record waste log
                                    val currentDate = SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(Date())

                                    val wasteLog = Entity_WasteLog(
                                        productFirebaseId = product.firebaseId,
                                        productName = product.name,
                                        category = product.category,
                                        quantity = quantity,
                                        reason = reason,
                                        wasteDate = currentDate,
                                        recordedBy = UserSession.getUserFullName()
                                    )

                                    wasteLogViewModel.insertWasteLog(wasteLog)

                                    // Deduct from Inventory B only
                                    val newInventoryB = (product.inventoryB - quantity).coerceAtLeast(0)
                                    val newQuantity = product.inventoryA + newInventoryB

                                    productViewModel.updateProduct(
                                        product.copy(
                                            inventoryB = newInventoryB,
                                            quantity = newQuantity
                                        )
                                    )

                                    // Log audit
                                    AuditHelper.logWaste(product.name, quantity)

                                    wasteSuccessMessage = "Marked $quantity units of ${product.name} as waste"
                                    showSuccessDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Success Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Waste Recorded", color = Color(0xFF8B4513), fontWeight = FontWeight.Bold) },
                text = { Text(wasteSuccessMessage) },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4513))
                    ) {
                        Text("OK")
                    }
                },
                containerColor = Color(0xFFFFE4B5)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteProductCard(
    product: Entity_Products,
    productViewModel: ProductViewModel,
    onMarkAsWaste: (quantity: Int, reason: String) -> Unit
) {
    var wasteQuantity by remember { mutableStateOf("") }
    var wasteReason by remember { mutableStateOf("End of day waste") }
    var showWasteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Image(
                painter = rememberAsyncImagePainter(
                    model = product.imageUri.ifEmpty { R.drawable.img }
                ),
                contentDescription = product.name,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF8B4513)
                )
                Text(
                    product.category,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Inventory B: ${product.inventoryB} units",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513)
                )
            }

            // Mark Waste Button
            Button(
                onClick = { showWasteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Delete, "Waste", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Waste", fontSize = 13.sp)
            }
        }
    }

    // Waste Dialog
    if (showWasteDialog) {
        AlertDialog(
            onDismissRequest = { showWasteDialog = false },
            title = {
                Text(
                    "Mark ${product.name} as Waste",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            },
            text = {
                Column {
                    Text("Record expired/damaged items for audit trail")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Available in Inventory B: ${product.inventoryB} units",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = wasteQuantity,
                        onValueChange = { wasteQuantity = it },
                        label = { Text("Quantity to Waste") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reason dropdown
                    val wasteReasons = listOf(
                        "End of day waste",
                        "Expired",
                        "Damaged",
                        "Quality issue",
                        "Customer return",
                        "Other"
                    )
                    var expandedReason by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedReason,
                        onExpandedChange = { expandedReason = !expandedReason }
                    ) {
                        OutlinedTextField(
                            value = wasteReason,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reason") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedReason) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedReason,
                            onDismissRequest = { expandedReason = false }
                        ) {
                            wasteReasons.forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason) },
                                    onClick = {
                                        wasteReason = reason
                                        expandedReason = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = wasteQuantity.toIntOrNull()
                        if (qty != null && qty > 0 && qty <= product.inventoryB) {
                            onMarkAsWaste(qty, wasteReason)
                            showWasteDialog = false
                            wasteQuantity = ""
                            wasteReason = "End of day waste"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Mark as Waste")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showWasteDialog = false
                    wasteQuantity = ""
                    wasteReason = "End of day waste"
                }) {
                    Text("Cancel", color = Color(0xFF8B4513))
                }
            },
            containerColor = Color(0xFFFFE4B5)
        )
    }
}
