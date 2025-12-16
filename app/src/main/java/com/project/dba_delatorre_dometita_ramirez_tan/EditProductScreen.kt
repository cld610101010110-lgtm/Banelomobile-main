package com.project.dba_delatorre_dometita_ramirez_tan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    // State holders
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var isPerishable by remember { mutableStateOf(false) }
    var shelfLifeDays by remember { mutableStateOf("") }
    var shelfLifeError by remember { mutableStateOf(false) }
    var currentImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val displayImageUrl = uploadedImageUrl ?: currentImageUrl

    // Dropdown state
    var expandedCategory by remember { mutableStateOf(false) }
    val categories = listOf("Ingredients", "Beverages", "Pastries")

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Update states when product changes
    LaunchedEffect(productToEdit.firebaseId) {
        android.util.Log.d("EditProductScreen", "Loading product: ${productToEdit.name}")
        android.util.Log.d("EditProductScreen", "Product firebaseId: ${productToEdit.firebaseId}")
        android.util.Log.d("EditProductScreen", "Product imageUri: ${productToEdit.image_uri}")

        name = TextFieldValue(productToEdit.name)
        category = productToEdit.category
        price = productToEdit.price.toString()
        quantity = productToEdit.quantity.toString()
        isPerishable = productToEdit.isPerishable
        shelfLifeDays = productToEdit.shelfLifeDays.toString()
        currentImageUrl = productToEdit.image_uri
        selectedImageUri = null
        uploadedImageUrl = null
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            isUploadingImage = true

            // ✅ Upload to Cloudinary
            scope.launch {
                try {
                    android.util.Log.d("EditProductScreen", "📤 Uploading image to Cloudinary...")
                    val cloudinaryUrl = CloudinaryHelper.uploadImage(it)
                    uploadedImageUrl = cloudinaryUrl
                    isUploadingImage = false
                    android.util.Log.d("EditProductScreen", "✅ Image uploaded successfully: $cloudinaryUrl")
                } catch (e: Exception) {
                    android.util.Log.e("EditProductScreen", "❌ Cloudinary upload failed: ${e.message}")
                    isUploadingImage = false
                    selectedImageUri = null
                }
            }
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
                        ) {
                            // ✅ Display image: uploaded image > current image > placeholder


                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray)
                                    .clickable {
                                        try {
                                            imagePickerLauncher.launch("image/*")
                                        } catch (e: Exception) {
                                            android.util.Log.e("EditProductScreen", "Image picker launch failed: ${e.message}")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (displayImageUrl != null && displayImageUrl.isNotEmpty()) {
                                    android.util.Log.d("EditProductScreen", "📸 Displaying image: $displayImageUrl")
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = displayImageUrl,
                                            error = painterResource(R.drawable.ic_launcher_foreground),
                                            placeholder = painterResource(R.drawable.ic_launcher_foreground),
                                            onSuccess = {
                                                android.util.Log.d("EditProductScreen", "✅ Image loaded successfully: $displayImageUrl")
                                            },
                                            onError = { error ->
                                                android.util.Log.e("EditProductScreen", "❌ Image load failed for: $displayImageUrl")
                                                android.util.Log.e("EditProductScreen", "Error: ${error.result.throwable?.message}")
                                                error.result.throwable?.printStackTrace()
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

                            // Show loading indicator while uploading
                            if (isUploadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
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

                        Text(
                            text = if (displayImageUrl != null) "Tap to change product image" else "Tap to add product image",
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

                        // ✅ PERISHABLE SECTION - Only for Ingredients
                        if (category.equals("Ingredients", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Perishable Settings", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B3E2E))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isPerishable,
                                    onCheckedChange = {
                                        isPerishable = it
                                        shelfLifeError = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("This product is perishable", fontSize = 14.sp)
                            }

                            if (isPerishable) {
                                OutlinedTextField(
                                    value = shelfLifeDays,
                                    onValueChange = {
                                        // Only allow digits
                                        if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                            shelfLifeDays = it
                                            shelfLifeError = false
                                        }
                                    },
                                    label = { Text("Shelf Life (days)") },
                                    isError = shelfLifeError,
                                    modifier = textFieldModifier,
                                    shape = RoundedCornerShape(20.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                if (shelfLifeError) Text("Required", color = Color.Red, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                // ✅ Validate shelf life if perishable
                                val isShelfLifeValid = !isPerishable || shelfLifeDays.isNotBlank()

                                if (!isShelfLifeValid) {
                                    shelfLifeError = true
                                } else {
                                    shelfLifeError = false

                                    // ✅ Use uploadedImageUrl if new image was uploaded, otherwise keep current
                                    val finalImageUri = uploadedImageUrl ?: currentImageUrl ?: ""

                                    // ✅ FIX: When adding quantity to ingredients, it should go to Inventory A
                                    val isIngredient = category.equals("Ingredients", ignoreCase = true)
                                    val quantityValue = quantity.toIntOrNull() ?: 0
                                    val shelfLifeValue = if (isPerishable) shelfLifeDays.toIntOrNull() ?: 0 else 0

                                    val updatedProduct = Entity_Products(
                                        id = productToEdit.id,
                                        firebaseId = productToEdit.firebaseId,
                                        name = name.text,
                                        category = category,
                                        price = price.toDoubleOrNull() ?: 0.0,
                                        quantity = quantityValue,
                                        isPerishable = isPerishable,
                                        shelfLifeDays = shelfLifeValue,
                                        // ✅ For ingredients: put stock in Inventory A, keep existing B
                                        // For other products: keep existing inventory values
                                        inventoryA = if (isIngredient) quantityValue else productToEdit.inventoryA,
                                        inventoryB = productToEdit.inventoryB, // Keep existing B value
                                        costPerUnit = productToEdit.costPerUnit,
                                        image_uri = finalImageUri // ✅ Use Cloudinary URL
                                    )

                                    android.util.Log.d("EditProductScreen", "Saving product with imageUri: $finalImageUri")
                                    android.util.Log.d("EditProductScreen", "Inventory A: ${updatedProduct.inventoryA}, B: ${updatedProduct.inventoryB}")
                                    android.util.Log.d("EditProductScreen", "Perishable: ${updatedProduct.isPerishable}, ShelfLife: ${updatedProduct.shelfLifeDays} days")
                                    viewModel3.updateProduct(updatedProduct)
                                    AuditHelper.logProductEdit(name.text)
                                    android.util.Log.d("EditProductScreen", "✅ Audit trail logged for product edit")

                                    navController.popBackStack()
                                }
                            },
                            enabled = !isUploadingImage, // ✅ Disable while uploading
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6F4E37),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }

                    }
                }
            }
        }
    )
}
