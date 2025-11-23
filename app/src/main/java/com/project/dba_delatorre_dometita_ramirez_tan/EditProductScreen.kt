package com.project.dba_delatorre_dometita_ramirez_tan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    navController: NavController,
    viewModel3: ProductViewModel,
    productToEdit: Entity_Products
) {
    // ✅ Use simple remember without keys
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf(TextFieldValue("")) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Dropdown state
    var expandedCategory by remember { mutableStateOf(false) }
    val categories = listOf("Ingredients", "Beverages", "Pastries")

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✅ Update states when product changes
    LaunchedEffect(productToEdit.firebaseId) {
        android.util.Log.d("EditProductScreen", "Loading product: ${productToEdit.name}")
        android.util.Log.d("EditProductScreen", "Product firebaseId: ${productToEdit.firebaseId}")
        android.util.Log.d("EditProductScreen", "Product imageUri: ${productToEdit.imageUri}")

        name = TextFieldValue(productToEdit.name)
        category = productToEdit.category
        price = productToEdit.price.toString()
        quantity = productToEdit.quantity.toString()
        imageUri = TextFieldValue(productToEdit.imageUri)
        selectedImageUri = null
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageUri = TextFieldValue(it.toString())
            android.util.Log.d("EditProductScreen", "New image selected: $it")
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    Scaffold(
        containerColor = Color.Transparent,
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {

                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Product Information", fontSize = 24.sp, color = Color(0xFF6B3E2E))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Update your product details", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.LightGray)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val imageModel = if (selectedImageUri != null) {
                                selectedImageUri  // Show newly selected image
                            } else {
                                imageUri.text     // Show existing Firebase URL
                            }

                            // ✅ Log what we're trying to load
                            LaunchedEffect(imageModel) {
                                android.util.Log.d("EditProductScreen", "Attempting to load image: $imageModel")
                            }

                            if (imageUri.text.isNotEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = imageModel,
                                        error = painterResource(R.drawable.ic_launcher_foreground),
                                        placeholder = painterResource(R.drawable.ic_launcher_foreground),
                                        onError = { error ->
                                            android.util.Log.e("EditProductScreen", "Image load failed: ${error.result.throwable.message}")
                                        }
                                    ),
                                    contentDescription = "Product Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = "No Image\nTap to select",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        Text(
                            text = if (imageUri.text.isNotEmpty()) "Tap to change product image" else "Tap to add product image",
                            fontSize = 12.sp,
                            color = Color(0xFF4B3832),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val textFieldModifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Product Name") },
                            modifier = textFieldModifier,
                            shape = RoundedCornerShape(20.dp)
                        )

                        // CATEGORY DROPDOWN
                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory },
                            modifier = textFieldModifier
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            expandedCategory = false
                                        }
                                    )
                                }
                            }
                        }

                        // PRICE FIELD - Numeric only with decimal
                        OutlinedTextField(
                            value = price,
                            onValueChange = {
                                // Only allow digits and single decimal point
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    price = it
                                }
                            },
                            label = { Text("Price") },
                            modifier = textFieldModifier,
                            shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        // QUANTITY FIELD - Integers only
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = {
                                // Only allow digits (no decimal for quantity)
                                if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                    quantity = it
                                }
                            },
                            label = { Text("Quantity") },
                            modifier = textFieldModifier,
                            shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                // Determine which image URI to use
                                val finalImageUri = if (selectedImageUri != null) {
                                    // User selected a NEW image - use the URI directly (don't copy to internal storage)
                                    // ProductRepository.update() will upload it to Firebase Storage
                                    selectedImageUri.toString()
                                } else {
                                    // Keep existing Firebase URL
                                    imageUri.text
                                }

                                val updatedProduct = Entity_Products(
                                    id = productToEdit.id,
                                    firebaseId = productToEdit.firebaseId,
                                    name = name.text,
                                    category = category,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    quantity = quantity.toIntOrNull() ?: 0,
                                    imageUri = finalImageUri
                                )

                                android.util.Log.d("EditProductScreen", "Saving product with imageUri: $finalImageUri")
                                viewModel3.updateProduct(updatedProduct)
                                AuditHelper.logProductEdit(name.text)
                                android.util.Log.d("EditProductScreen", "✅ Audit trail logged for product edit")

                                navController.popBackStack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6F4E37),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    )
}