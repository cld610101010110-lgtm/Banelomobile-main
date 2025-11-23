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
import androidx.compose.material.icons.filled.ArrowForward
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTransferScreen(
    navController: NavController,
    productViewModel: ProductViewModel
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var transferSuccessMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        productViewModel.getAllProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Inventory (A → B)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6F4E37)
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
                    containerColor = Color(0xFFEEE0CB)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Transfer Stock from Warehouse to Display",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F4E37)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Inventory A = Main/Warehouse stock\n• Inventory B = Display/Expendable stock\n• Orders deduct from B first",
                        fontSize = 13.sp,
                        color = Color(0xFF4E342E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Products List
            if (productViewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6F4E37))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(productViewModel.productList.filter { it.inventoryA > 0 }) { product ->
                        TransferProductCard(
                            product = product,
                            onTransfer = { quantity ->
                                productViewModel.transferInventory(product.firebaseId, quantity) { result ->
                                    if (result.isSuccess) {
                                        transferSuccessMessage = "Transferred $quantity units of ${product.name} to display inventory"
                                        showSuccessDialog = true
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Transfer failed"
                                        showErrorDialog = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Success Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Transfer Successful", color = Color(0xFF6F4E37), fontWeight = FontWeight.Bold) },
                text = { Text(transferSuccessMessage) },
                confirmButton = {
                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F4E37))
                    ) {
                        Text("OK")
                    }
                },
                containerColor = Color(0xFFEEE0CB)
            )
        }

        // Error Dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Transfer Failed", color = Color.Red, fontWeight = FontWeight.Bold) },
                text = { Text(errorMessage) },
                confirmButton = {
                    Button(
                        onClick = { showErrorDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("OK")
                    }
                },
                containerColor = Color(0xFFEEE0CB)
            )
        }
    }
}

@Composable
fun TransferProductCard(
    product: Entity_Products,
    onTransfer: (Int) -> Unit
) {
    var transferQuantity by remember { mutableStateOf("") }
    var showTransferDialog by remember { mutableStateOf(false) }

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
                    color = Color(0xFF6F4E37)
                )
                Text(
                    product.category,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        "Inv A: ${product.inventoryA}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E342E)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Inv B: ${product.inventoryB}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Transfer Button
            Button(
                onClick = { showTransferDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6F4E37)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.ArrowForward, "Transfer", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Transfer", fontSize = 13.sp)
            }
        }
    }

    // Transfer Dialog
    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = {
                Text(
                    "Transfer ${product.name}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6F4E37)
                )
            },
            text = {
                Column {
                    Text("Move stock from Warehouse (A) to Display (B)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Available in Inventory A: ${product.inventoryA} units",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F4E37)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = transferQuantity,
                        onValueChange = { transferQuantity = it },
                        label = { Text("Quantity to Transfer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = transferQuantity.toIntOrNull()
                        if (qty != null && qty > 0 && qty <= product.inventoryA) {
                            onTransfer(qty)
                            showTransferDialog = false
                            transferQuantity = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F4E37))
                ) {
                    Text("Transfer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTransferDialog = false
                    transferQuantity = ""
                }) {
                    Text("Cancel", color = Color(0xFF6F4E37))
                }
            },
            containerColor = Color(0xFFEEE0CB)
        )
    }
}
