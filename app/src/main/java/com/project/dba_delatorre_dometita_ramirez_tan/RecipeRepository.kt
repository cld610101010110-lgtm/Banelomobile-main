package com.project.dba_delatorre_dometita_ramirez_tan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeRepository(
    private val database: Database_Products
) {
    private val daoRecipe = database.daoRecipe()
    private val daoProducts = database.dao_products()

    companion object {
        private const val TAG = "RecipeRepository"
    }

    // ============ SYNC FROM API ============

    suspend fun syncRecipesFromApi(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Starting recipe sync from API...")

                val recipesResult = BaneloApiService.safeCall {
                    BaneloApiService.api.getAllRecipes()
                }

                if (recipesResult.isSuccess) {
                    val recipes = recipesResult.getOrNull() ?: return@withContext Result.success(Unit)

                    Log.d(TAG, "📋 Fetched ${recipes.size} recipes")

                    // Clear existing data
                    daoRecipe.clearAllIngredients()
                    daoRecipe.clearAllRecipes()

                    var recipesInserted = 0
                    var ingredientsInserted = 0

                    // Insert each recipe and its ingredients
                    recipes.forEach { apiRecipe ->
                        // Insert recipe and get the generated ID
                        val recipe = Entity_Recipe(
                            recipeId = 0, // Let Room auto-generate
                            firebaseId = apiRecipe.firebaseId ?: "",
                            productId = apiRecipe.productFirebaseId ?: "",
                            productName = apiRecipe.productName ?: ""
                        )

                        val recipeId = daoRecipe.insertRecipe(recipe)
                        recipesInserted++

                        Log.d(TAG, "✅ Inserted recipe: ${recipe.productName} with ID: $recipeId")

                        // Insert ingredients for this recipe
                        val ingredients = apiRecipe.ingredients ?: emptyList()
                        if (ingredients.isNotEmpty()) {
                            // Filter out ingredients with missing required fields
                            val validIngredients = ingredients.filter { ing ->
                                !ing.ingredientFirebaseId.isNullOrBlank() && !ing.ingredientName.isNullOrBlank()
                            }

                            val ingredientEntities = validIngredients.map { ing ->
                                Entity_RecipeIngredient(
                                    id = 0, // Let Room auto-generate
                                    firebaseId = ing.firebaseId ?: "",
                                    recipeId = recipeId.toInt(),
                                    ingredientFirebaseId = ing.ingredientFirebaseId ?: "",
                                    ingredientName = ing.ingredientName ?: "",
                                    quantityNeeded = ing.quantityNeeded ?: 0.0,
                                    unit = ing.unit ?: "g"
                                )
                            }

                            daoRecipe.insertAllIngredients(ingredientEntities)
                            ingredientsInserted += ingredientEntities.size

                            Log.d(TAG, "   📦 Inserted ${ingredientEntities.size} ingredients")
                        }
                    }

                    Log.d(TAG, "✅ Recipe sync completed successfully")
                    Log.d(TAG, "   Recipes: $recipesInserted")
                    Log.d(TAG, "   Ingredients: $ingredientsInserted")

                    Result.success(Unit)
                } else {
                    Log.e(TAG, "❌ API sync failed: ${recipesResult.exceptionOrNull()?.message}")
                    Result.failure(recipesResult.exceptionOrNull() ?: Exception("Unknown error"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Recipe sync failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ============ READ OPERATIONS ============

    suspend fun getAllRecipesWithIngredients(): List<RecipeWithIngredients> {
        return try {
            daoRecipe.getAllRecipesWithIngredients()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipes: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getRecipeForProduct(productId: String): RecipeWithIngredients? {
        return try {
            daoRecipe.getRecipeWithIngredients(productId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipe for product $productId: ${e.message}", e)
            null
        }
    }

    // ============ CALCULATE AVAILABLE QUANTITY ============

    // ✅ Calculate max servings based on TOTAL stock (inventoryA + inventoryB)
    // Used for: InventoryListScreen - to show overall available servings
    suspend fun calculateMaxServings(productId: String): Int {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🧮 Calculating max servings (TOTAL stock)...")
            Log.d(TAG, "Product ID: $productId")

            if (productId.isBlank()) {
                Log.w(TAG, "❌ Product ID is blank! Cannot find recipe.")
                return 0
            }

            val recipe = daoRecipe.getRecipeByProductId(productId)

            if (recipe == null) {
                Log.w(TAG, "❌ NO RECIPE FOUND IN ROOM!")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                return 0
            }

            Log.d(TAG, "✅ Recipe found: ${recipe.productName}")

            val ingredients = daoRecipe.getIngredientsByRecipeId(recipe.recipeId)

            if (ingredients.isEmpty()) {
                Log.w(TAG, "❌ No ingredients found for this recipe!")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                return 0
            }

            Log.d(TAG, "📦 Found ${ingredients.size} ingredients:")

            val maxServingsPerIngredient = ingredients.map { ingredient ->
                val ingredientProduct = daoProducts.getProductByFirebaseId(ingredient.ingredientFirebaseId)

                if (ingredientProduct == null) {
                    Log.e(TAG, "     ❌ Product not found for ingredient: ${ingredient.ingredientName}")
                    return@map 0
                }

                val available = ingredientProduct.quantity.toDouble() // Total: A + B
                val needed = ingredient.quantityNeeded
                val maxServings = if (needed > 0) (available / needed).toInt() else 0

                Log.d(TAG, "  ${ingredient.ingredientName}: total=$available, needed=$needed, maxServings=$maxServings")
                maxServings
            }

            val result = maxServingsPerIngredient.minOrNull() ?: 0
            Log.d(TAG, "🎯 RESULT: Can make $result servings (based on TOTAL stock)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")

            result

        } catch (e: Exception) {
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "❌ Error calculating servings!")
            Log.e(TAG, "Error: ${e.message}", e)
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            0
        }
    }

    // ✅ NEW METHOD: Calculate max servings based ONLY on Inventory B
    // Used for: OrderProcessScreen - customers can only order what's in display inventory
    suspend fun calculateMaxServingsFromInventoryB(productId: String): Int {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🧮 Calculating max servings (INVENTORY B ONLY)...")
            Log.d(TAG, "Product ID: $productId")

            if (productId.isBlank()) {
                Log.w(TAG, "❌ Product ID is blank! Cannot find recipe.")
                return 0
            }

            val recipe = daoRecipe.getRecipeByProductId(productId)

            if (recipe == null) {
                Log.w(TAG, "❌ NO RECIPE FOUND IN ROOM!")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                return 0
            }

            Log.d(TAG, "✅ Recipe found: ${recipe.productName}")

            val ingredients = daoRecipe.getIngredientsByRecipeId(recipe.recipeId)

            if (ingredients.isEmpty()) {
                Log.w(TAG, "❌ No ingredients found for this recipe!")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                return 0
            }

            Log.d(TAG, "📦 Found ${ingredients.size} ingredients:")

            val maxServingsPerIngredient = ingredients.map { ingredient ->
                val ingredientProduct = daoProducts.getProductByFirebaseId(ingredient.ingredientFirebaseId)

                if (ingredientProduct == null) {
                    Log.e(TAG, "     ❌ Product not found for ingredient: ${ingredient.ingredientName}")
                    return@map 0
                }

                // ✅ KEY CHANGE: Use ONLY inventoryB instead of total quantity
                val available = ingredientProduct.inventoryB.toDouble()
                val needed = ingredient.quantityNeeded
                val maxServings = if (needed > 0) (available / needed).toInt() else 0

                Log.d(TAG, "  ${ingredient.ingredientName}: invB=$available, needed=$needed, maxServings=$maxServings")
                maxServings
            }

            val result = maxServingsPerIngredient.minOrNull() ?: 0
            Log.d(TAG, "🎯 RESULT: Can make $result servings (based on INVENTORY B ONLY)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")

            result

        } catch (e: Exception) {
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "❌ Error calculating servings!")
            Log.e(TAG, "Error: ${e.message}", e)
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            0
        }
    }

    // ============ DEDUCT INGREDIENTS ON ORDER ============

    suspend fun deductIngredients(productId: String, quantity: Int, saveToSales: (Entity_SalesReport) -> Unit) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔻 Deducting ingredients for $quantity servings")
            Log.d(TAG, "Product ID: $productId")

            val recipe = daoRecipe.getRecipeByProductId(productId)

            if (recipe == null) {
                Log.w(TAG, "⚠️ No recipe found, cannot deduct ingredients")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                return
            }

            Log.d(TAG, "📋 Recipe found: ${recipe.productName}")

            val ingredients = daoRecipe.getIngredientsByRecipeId(recipe.recipeId)

            if (ingredients.isEmpty()) {
                Log.w(TAG, "⚠️ No ingredients found for this recipe")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
                return
            }

            Log.d(TAG, "📦 Deducting ${ingredients.size} ingredients:")

            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

            ingredients.forEach { ingredient ->
                val product = daoProducts.getProductByFirebaseId(ingredient.ingredientFirebaseId)

                if (product != null) {
                    val amountToDeduct = (ingredient.quantityNeeded * quantity).toInt()

                    Log.d(TAG, "  📉 ${ingredient.ingredientName}:")
                    Log.d(TAG, "     Before - Inventory A: ${product.inventoryA}, Inventory B: ${product.inventoryB}")
                    Log.d(TAG, "     Deducting: $amountToDeduct ${ingredient.unit}")

                    var remainingToDeduct = amountToDeduct
                    var newInventoryA = product.inventoryA
                    var newInventoryB = product.inventoryB

                    if (newInventoryB > 0) {
                        val deductFromB = minOf(remainingToDeduct, newInventoryB)
                        newInventoryB -= deductFromB
                        remainingToDeduct -= deductFromB
                        Log.d(TAG, "     Deducted $deductFromB from Inventory B")
                    }

                    if (remainingToDeduct > 0 && newInventoryA > 0) {
                        val deductFromA = minOf(remainingToDeduct, newInventoryA)
                        newInventoryA -= deductFromA
                        remainingToDeduct -= deductFromA
                        Log.d(TAG, "     Deducted $deductFromA from Inventory A")
                    }

                    val newQuantity = newInventoryA + newInventoryB
                    Log.d(TAG, "     After - Inventory A: $newInventoryA, Inventory B: $newInventoryB, Total: $newQuantity")

                    val updatedProduct = product.copy(
                        quantity = newQuantity,
                        inventoryA = newInventoryA,
                        inventoryB = newInventoryB
                    )
                    daoProducts.updateProduct(updatedProduct)

                    val request = ProductRequest(
                        name = updatedProduct.name,
                        category = updatedProduct.category,
                        price = updatedProduct.price,
                        quantity = updatedProduct.quantity,
                        inventory_a = newInventoryA,
                        inventory_b = newInventoryB,
                        cost_per_unit = updatedProduct.costPerUnit,
                        image_uri = updatedProduct.image_uri ?: ""
                    )

                    val apiResult = BaneloApiService.safeCall {
                        BaneloApiService.api.updateProduct(product.firebaseId, request)
                    }

                    // ✅ Check API result before logging success
                    if (apiResult.isSuccess) {
                        Log.d(TAG, "     ✅ Updated in Room and API")
                    } else {
                        Log.d(TAG, "     ✅ Updated in Room")
                        Log.w(TAG, "     ⚠️ API sync failed: ${apiResult.exceptionOrNull()?.message}")
                    }

                    val ingredientSale = Entity_SalesReport(
                        productName = product.name,
                        category = product.category,
                        quantity = amountToDeduct,
                        price = 0.0,
                        orderDate = currentDate,
                        productFirebaseId = product.firebaseId
                    )

                    saveToSales(ingredientSale)
                    Log.d(TAG, "     💰 Ingredient sale recorded: $amountToDeduct ${ingredient.unit}")

                } else {
                    Log.w(TAG, "  ⚠️ Product not found for ingredient: ${ingredient.ingredientName}")
                }
            }

            Log.d(TAG, "✅ All ingredients deducted successfully")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "❌ Error deducting ingredients!")
            Log.e(TAG, "Error: ${e.message}", e)
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    // ============ CALCULATE RECIPE COST ============

    data class IngredientCostInfo(
        val ingredientName: String,
        val quantityNeeded: Double,
        val unit: String,
        val costPerUnit: Double,
        val totalCost: Double
    )

    data class RecipeCostSummary(
        val ingredientCosts: List<IngredientCostInfo>,
        val totalCost: Double,
        val sellingPrice: Double,
        val profitMargin: Double,
        val profitPercentage: Double
    )

    suspend fun calculateRecipeCost(productId: String): RecipeCostSummary? {
        return try {
            Log.d(TAG, "💰 Calculating recipe cost for: $productId")

            val recipe = daoRecipe.getRecipeByProductId(productId)
            if (recipe == null) {
                Log.w(TAG, "⚠️ No recipe found")
                return null
            }

            val product = daoProducts.getProductByFirebaseId(productId)
            val sellingPrice = product?.price ?: 0.0

            val ingredients = daoRecipe.getIngredientsByRecipeId(recipe.recipeId)
            if (ingredients.isEmpty()) {
                Log.w(TAG, "⚠️ No ingredients found")
                return null
            }

            val ingredientCosts = ingredients.mapNotNull { ingredient ->
                val ingredientProduct = daoProducts.getProductByFirebaseId(ingredient.ingredientFirebaseId)

                if (ingredientProduct != null) {
                    val totalStockQuantity = ingredientProduct.quantity.toDouble()
                    val costPerUnit = if (totalStockQuantity > 0) {
                        ingredientProduct.price / totalStockQuantity
                    } else {
                        0.0
                    }

                    val totalCost = ingredient.quantityNeeded * costPerUnit

                    IngredientCostInfo(
                        ingredientName = ingredient.ingredientName,
                        quantityNeeded = ingredient.quantityNeeded,
                        unit = ingredient.unit,
                        costPerUnit = costPerUnit,
                        totalCost = totalCost
                    )
                } else {
                    null
                }
            }

            val totalCost = ingredientCosts.sumOf { it.totalCost }
            val profitMargin = sellingPrice - totalCost
            val profitPercentage = if (totalCost > 0) {
                (profitMargin / totalCost) * 100
            } else {
                0.0
            }

            Log.d(TAG, "✅ Total Cost: ₱${"%.2f".format(totalCost)}")
            Log.d(TAG, "   Selling Price: ₱${"%.2f".format(sellingPrice)}")
            Log.d(TAG, "   Profit: ₱${"%.2f".format(profitMargin)} (${"%.1f".format(profitPercentage)}%)")

            RecipeCostSummary(
                ingredientCosts = ingredientCosts,
                totalCost = totalCost,
                sellingPrice = sellingPrice,
                profitMargin = profitMargin,
                profitPercentage = profitPercentage
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calculating recipe cost: ${e.message}", e)
            null
        }
    }
}