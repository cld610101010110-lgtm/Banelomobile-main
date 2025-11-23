package com.project.dba_delatorre_dometita_ramirez_tan

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientCostViewScreen(
    navController: NavController,
    productViewModel: ProductViewModel,
    recipeViewModel: RecipeViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Fetch all products
    LaunchedEffect(Unit) {
        productViewModel.getAllProducts()
    }

    // Filter only recipe-based products (Beverages and Pastries)
    val recipeBasedProducts = productViewModel.productList.filter { product ->
        product.category.equals("Beverages", ignoreCase = true) ||
        product.category.equals("Pastries", ignoreCase = true)
    }.sortedBy { it.name }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3D3BD), Color(0xFF837060))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { SidebarDrawer(navController) }
    ) {
        Scaffold(
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
                                text = "Ingredient Cost Analysis",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                navController.popBackStack()
                            }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = gradient)
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (recipeBasedProducts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recipe-based products found.\nAdd Beverages or Pastries with recipes!",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "Recipe-based products with cost breakdowns:",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        recipeBasedProducts.forEach { product ->
                            var costSummary by remember { mutableStateOf<RecipeRepository.RecipeCostSummary?>(null) }
                            var isLoading by remember { mutableStateOf(true) }

                            LaunchedEffect(product.firebaseId) {
                                isLoading = true
                                costSummary = recipeViewModel.getRecipeCost(product.firebaseId)
                                isLoading = false
                            }

                            RecipeCostCard(
                                productName = product.name,
                                category = product.category,
                                costSummary = costSummary,
                                isLoading = isLoading
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun RecipeCostCard(
    productName: String,
    category: String,
    costSummary: RecipeRepository.RecipeCostSummary?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Product Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = productName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    )
                    Text(
                        text = category,
                        fontSize = 14.sp,
                        color = Color(0xFF6D4C41),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6F4E37))
                }
            } else if (costSummary == null) {
                Text(
                    text = "No recipe found for this product",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                // Ingredient Breakdown Header
                Text(
                    text = "Ingredient Breakdown:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E342E),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Ingredients List
                costSummary.ingredientCosts.forEach { ingredient ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = ingredient.ingredientName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Qty: ${ingredient.quantityNeeded} ${ingredient.unit}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6D4C41)
                                )
                                Text(
                                    text = "@ ₱${String.format("%.2f", ingredient.costPerUnit)}/${ingredient.unit}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8B4513)
                                )
                            }

                            Text(
                                text = "Cost: ₱${String.format("%.2f", ingredient.totalCost)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3E2723),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color(0xFFBCAAA4), thickness = 1.dp)

                Spacer(modifier = Modifier.height(16.dp))

                // Cost Summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Cost Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Recipe Cost:", fontSize = 15.sp, color = Color(0xFF5D4037))
                        Text(
                            "₱${String.format("%.2f", costSummary.totalCost)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Selling Price:", fontSize = 15.sp, color = Color(0xFF5D4037))
                        Text(
                            "₱${String.format("%.2f", costSummary.sellingPrice)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Divider(color = Color(0xFFBCAAA4), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Profit Margin:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text(
                            "₱${String.format("%.2f", costSummary.profitMargin)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (costSummary.profitMargin >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Profit Percentage:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Text(
                            "${String.format("%.1f", costSummary.profitPercentage)}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (costSummary.profitPercentage >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}
