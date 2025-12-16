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
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel3: ProductViewModel,
    onUserSaved: () -> Unit = {}
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    var productName by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    // 🆕 PERISHABLE FIELDS
    var isPerishable by remember { mutableStateOf(false) }
    var shelfLifeDays by remember { mutableStateOf("") }
    var shelfLifeError by remember { mutableStateOf(false) }

    // Dropdown state
    var expandedCategory by remember { mutableStateOf(false) }
    val categories = listOf("Ingredients", "Beverages", "Pastries")

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageError = false
            // 🆕 FIX: Don't upload immediately - just store the URI
            android.util.Log.d("AddProductScreen", "📸 Image selected (not uploaded yet): $it")
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(gradient)
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }

                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Add Product", fontSize = 24.sp, color = Color(0xFF6B3E2E))
                            Spacer(modifier = Modifier.height(16.dp))

                            // IMAGE SELECTION SECTION
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(8.dp)
                            ) {
                                if (uploadedImageUrl != null || selectedImageUri != null) {
                                    // Show uploaded image or preview
                                    val imageModel = uploadedImageUrl ?: selectedImageUri
                                    android.util.Log.d("AddProductScreen", "📸 Displaying image: $imageModel")
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = imageModel,
                                            onSuccess = {
                                                android.util.Log.d("AddProductScreen", "✅ Image loaded successfully")
                                            },
                                            onError = { error ->
                                                android.util.Log.e("AddProductScreen", "❌ Image load failed: ${error.result.throwable?.message}")
                                            }
                                        ),
                                        contentDescription = "Selected Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                try {
                                                    imagePickerLauncher.launch("image/*")
                                                } catch (e: Exception) {
                                                    android.util.Log.e("AddProductScreen", "Image picker launch failed: ${e.message}")
                                                }
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Show placeholder
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color(0xFFE0C3A1), Color(0xFFB1785F))
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { imagePickerLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Product Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Tap to choose image",
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }

                                // Show loading indicator while uploading
                                if (isUploadingImage) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color.White)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Uploading...", color = Color.White, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }

                            if (imageError) {
                                Text("Image is required", color = Color.Red, fontSize = 12.sp)
                            }

                            val textFieldModifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)

                            OutlinedTextField(
                                value = productName,
                                onValueChange = {
                                    productName = it; nameError = false
                                },
                                label = { Text("Product Name") },
                                isError = nameError,
                                modifier = textFieldModifier,
                                shape = RoundedCornerShape(20.dp)
                            )
                            if (nameError) Text("Required", color = Color.Red, fontSize = 12.sp)

                            // CATEGORY DROPDOWN
                            ExposedDropdownMenuBox(
                                expanded = expandedCategory,
                                onExpandedChange = { expandedCategory = !expandedCategory },
                                modifier = textFieldModifier
                            ) {
                                OutlinedTextField(
                                    value = productCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown"
                                        )
                                    },
                                    isError = categoryError,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCategory,
                                    onDismissRequest = { expandedCategory = false }
                                ) {
                                    categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category) },
                                            onClick = {
                                                productCategory = category
                                                expandedCategory = false
                                                categoryError = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (categoryError) Text("Required", color = Color.Red, fontSize = 12.sp)

                            // PRICE FIELD - Numeric only with decimal
                            OutlinedTextField(
                                value = productPrice,
                                onValueChange = {
                                    // Only allow digits and single decimal point
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        productPrice = it
                                        priceError = false
                                    }
                                },
                                label = { Text("Price") },
                                isError = priceError,
                                modifier = textFieldModifier,
                                shape = RoundedCornerShape(20.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            if (priceError) Text("Required", color = Color.Red, fontSize = 12.sp)

                            // QUANTITY FIELD - Integers only
                            OutlinedTextField(
                                value = productQuantity,
                                onValueChange = {
                                    // Only allow digits (no decimal for quantity)
                                    if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                        productQuantity = it
                                        quantityError = false
                                    }
                                },
                                label = { Text("Quantity") },
                                isError = quantityError,
                                modifier = textFieldModifier,
                                shape = RoundedCornerShape(20.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            if (quantityError) Text("Required", color = Color.Red, fontSize = 12.sp)

                            // 🆕 PERISHABLE SECTION - Only show for Ingredients category
                            if (productCategory.equals("Ingredients", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "Perishable Product",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B3E2E)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isPerishable,
                                        onCheckedChange = { isPerishable = it },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "This product is perishable",
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B3E2E)
                                    )
                                }

                                if (isPerishable) {
                                    OutlinedTextField(
                                        value = shelfLifeDays,
                                        onValueChange = {
                                            if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                                shelfLifeDays = it
                                                shelfLifeError = false
                                            }
                                        },
                                        label = { Text("Shelf Life (days)") },
                                        isError = shelfLifeError,
                                        modifier = textFieldModifier,
                                        shape = RoundedCornerShape(20.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        suffix = { Text("days") }
                                    )
                                    if (shelfLifeError) Text("Required if perishable", color = Color.Red, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    nameError = productName.isBlank()
                                    categoryError = productCategory.isBlank()
                                    priceError = productPrice.isBlank()
                                    quantityError = productQuantity.isBlank()
                                    imageError = selectedImageUri == null

                                    // 🆕 ADD: Check shelf life if perishable
                                    shelfLifeError = isPerishable && shelfLifeDays.isBlank()

                                    val isValid = !(nameError || categoryError || priceError || quantityError || imageError || shelfLifeError)

                                    if (isValid) {
                                        // 🆕 FIX: Wrap image upload in coroutine
                                        scope.launch {
                                            isUploadingImage = true

                                            // 🆕 UPLOAD IMAGE ONLY ON SAVE
                                            var finalImageUrl: String? = null
                                            try {
                                                android.util.Log.d("AddProductScreen", "📤 Uploading image to Cloudinary on save...")
                                                finalImageUrl = CloudinaryHelper.uploadImage(selectedImageUri!!)
                                                android.util.Log.d("AddProductScreen", "✅ Image uploaded: $finalImageUrl")
                                            } catch (e: Exception) {
                                                android.util.Log.e("AddProductScreen", "❌ Image upload failed: ${e.message}")
                                                imageError = true
                                                isUploadingImage = false
                                                return@launch
                                            }

                                            isUploadingImage = false

                                            if (finalImageUrl == null) {
                                                imageError = true
                                                return@launch
                                            }

                                            android.util.Log.d("AddProductScreen", "🆕 Creating new product...")
                                            android.util.Log.d("AddProductScreen", "Cloudinary image URL: $finalImageUrl")
                                            android.util.Log.d("AddProductScreen", "🔬 Perishable: $isPerishable, Shelf Life: $shelfLifeDays days")

                                            val qty = productQuantity.toInt()
                                            viewModel3.insertProduct(
                                                Entity_Products(
                                                    name = productName.trim(),
                                                    category = productCategory.trim(),
                                                    price = productPrice.toDouble(),
                                                    quantity = qty,
                                                    inventoryA = qty,
                                                    inventoryB = 0,
                                                    image_uri = finalImageUrl,
                                                    // 🆕 ADD PERISHABLE FIELDS:
                                                    isPerishable = isPerishable,
                                                    shelfLifeDays = if (isPerishable) shelfLifeDays.toInt() else 0
                                                )
                                            )
                                            AuditHelper.logProductAdd(productName.trim())
                                            android.util.Log.d("AddProductScreen", "✅ Audit trail logged for product add")
                                            showDialog = true
                                        }
                                    }
                                },
                                enabled = !isUploadingImage,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5D4037),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Save Product")
                            }


                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("Cancel", color = Color.Black)
                            }
                        }
                    }
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDialog = false
                                    navController.popBackStack()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5D4037),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Okay")
                            }
                        },
                        title = { Text("Success") },
                        text = { Text("Product saved successfully!") }
                    )
                }
            }
        }
    )
}
