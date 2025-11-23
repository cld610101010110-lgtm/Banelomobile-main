package com.project.dba_delatorre_dometita_ramirez_tan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.UUID

/**
 * PostgreSQL Adapter for Banelo POS
 *
 * This adapter implements ingredient-based inventory management:
 * - When a product (pastry/beverage) is sold, its ingredients are deducted from inventory
 * - Products themselves are not deducted, only their recipe ingredients
 * - Supports dual inventory system (Inventory A = warehouse, Inventory B = display)
 */
object PostgreSQLAdapter {

    // Connection configuration
    private const val DB_HOST = "10.0.2.2"  // Android emulator -> host machine
    private const val DB_PORT = "5432"
    private const val DB_NAME = "banelo_db"
    private const val DB_USER = "postgres"
    private const val DB_PASSWORD = "admin123"
    private val JDBC_URL = "jdbc:pgsql://$DB_HOST:$DB_PORT/$DB_NAME"  // pgjdbc-ng uses pgsql

    /**
     * Get database connection
     */
    private suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        Class.forName("com.impossibl.postgres.jdbc.PGDriver")  // pgjdbc-ng driver
        DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)
    }

    // ========================================================================
    // PRODUCTS
    // ========================================================================

    /**
     * Get all active products
     */
    suspend fun getAllProducts(): List<Entity_Products> = withContext(Dispatchers.IO) {
        val connection = getConnection()
        val query = """
            SELECT id, firebase_id, name, category, price, quantity,
                   inventory_a, inventory_b, cost_per_unit, image_uri
            FROM products
            WHERE is_active = true
            ORDER BY name
        """
        val statement = connection.prepareStatement(query)
        val resultSet = statement.executeQuery()

        val products = mutableListOf<Entity_Products>()
        while (resultSet.next()) {
            products.add(resultSet.toProduct())
        }

        resultSet.close()
        statement.close()
        connection.close()

        products
    }

    /**
     * Get products by category
     */
    suspend fun getProductsByCategory(category: String): List<Entity_Products> = withContext(Dispatchers.IO) {
        val connection = getConnection()
        val query = """
            SELECT id, firebase_id, name, category, price, quantity,
                   inventory_a, inventory_b, cost_per_unit, image_uri
            FROM products
            WHERE category = ? AND is_active = true
            ORDER BY name
        """
        val statement = connection.prepareStatement(query)
        statement.setString(1, category)
        val resultSet = statement.executeQuery()

        val products = mutableListOf<Entity_Products>()
        while (resultSet.next()) {
            products.add(resultSet.toProduct())
        }

        resultSet.close()
        statement.close()
        connection.close()

        products
    }

    /**
     * Get product by firebase ID
     */
    suspend fun getProductByFirebaseId(firebaseId: String): Entity_Products? = withContext(Dispatchers.IO) {
        val connection = getConnection()
        val query = """
            SELECT id, firebase_id, name, category, price, quantity,
                   inventory_a, inventory_b, cost_per_unit, image_uri
            FROM products
            WHERE firebase_id = ?
        """
        val statement = connection.prepareStatement(query)
        statement.setString(1, firebaseId)
        val resultSet = statement.executeQuery()

        val product = if (resultSet.next()) {
            resultSet.toProduct()
        } else null

        resultSet.close()
        statement.close()
        connection.close()

        product
    }

    /**
     * Insert new product
     */
    suspend fun insertProduct(product: Entity_Products): String = withContext(Dispatchers.IO) {
        val connection = getConnection()
        val uuid = UUID.randomUUID()
        val firebaseId = product.firebaseId.ifEmpty { UUID.randomUUID().toString() }

        val query = """
            INSERT INTO products (id, firebase_id, name, category, price, quantity,
                                 inventory_a, inventory_b, cost_per_unit, image_uri, description, sku, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        val statement = connection.prepareStatement(query)
        statement.setObject(1, uuid)
        statement.setString(2, firebaseId)
        statement.setString(3, product.name)
        statement.setString(4, product.category)
        statement.setDouble(5, product.price)
        statement.setInt(6, product.quantity)
        statement.setInt(7, product.inventoryA)
        statement.setInt(8, product.inventoryB)
        statement.setDouble(9, product.costPerUnit)
        statement.setString(10, product.imageUri)
        statement.setString(11, "")  // description
        statement.setString(12, "SKU-${System.currentTimeMillis()}")  // sku
        statement.setBoolean(13, true)  // is_active

        statement.executeUpdate()

        statement.close()
        connection.close()

        firebaseId
    }

    /**
     * Update product
     */
    suspend fun updateProduct(product: Entity_Products): Boolean = withContext(Dispatchers.IO) {
        val connection = getConnection()
        val query = """
            UPDATE products
            SET name = ?, category = ?, price = ?, quantity = ?,
                inventory_a = ?, inventory_b = ?, cost_per_unit = ?, image_uri = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE firebase_id = ?
        """
        val statement = connection.prepareStatement(query)
        statement.setString(1, product.name)
        statement.setString(2, product.category)
        statement.setDouble(3, product.price)
        statement.setInt(4, product.quantity)
        statement.setInt(5, product.inventoryA)
        statement.setInt(6, product.inventoryB)
        statement.setDouble(7, product.costPerUnit)
        statement.setString(8, product.imageUri)
        statement.setString(9, product.firebaseId)

        val rowsUpdated = statement.executeUpdate()

        statement.close()
        connection.close()

        rowsUpdated > 0
    }

    /**
     * Delete product (soft delete)
     */
    suspend fun deleteProduct(firebaseId: String): Boolean = withContext(Dispatchers.IO) {
        val connection = getConnection()
        val query = "UPDATE products SET is_active = false WHERE firebase_id = ?"
        val statement = connection.prepareStatement(query)
        statement.setString(1, firebaseId)

        val rowsUpdated = statement.executeUpdate()

        statement.close()
        connection.close()

        rowsUpdated > 0
    }

    // ========================================================================
    // INGREDIENT-BASED INVENTORY DEDUCTION
    // ========================================================================

    /**
     * Process sale with ingredient-based deduction
     *
     * When a product (e.g., Chocolate Cake) is sold:
     * 1. Record the sale with product name
     * 2. Get the recipe for that product
     * 3. Deduct each ingredient from inventory (Flour, Sugar, Butter, etc.)
     * 4. Deduct from Inventory B first, then Inventory A
     *
     * @param productFirebaseId The product being sold
     * @param quantitySold How many units sold
     * @param sale The sale record to insert
     * @return Result with success/failure message
     */
    suspend fun processSaleWithIngredientDeduction(
        productFirebaseId: String,
        quantitySold: Int,
        sale: Entity_SalesReport
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()
            connection.autoCommit = false  // Start transaction

            android.util.Log.d("PostgreSQL", "━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("PostgreSQL", "💰 Processing sale with ingredient deduction")
            android.util.Log.d("PostgreSQL", "Product ID: $productFirebaseId")
            android.util.Log.d("PostgreSQL", "Quantity sold: $quantitySold")

            // Step 1: Insert sale record
            val saleId = UUID.randomUUID()
            val insertSaleQuery = """
                INSERT INTO sales (id, firebase_id, order_id, product_name, category, quantity,
                                  price, order_date, product_firebase_id, payment_mode,
                                  gcash_reference_id, cashier_username)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?,
                        (SELECT id FROM products WHERE firebase_id = ? LIMIT 1),
                        ?, ?, ?)
            """
            val saleStmt = connection.prepareStatement(insertSaleQuery)
            saleStmt.setObject(1, saleId)
            saleStmt.setString(2, UUID.randomUUID().toString())
            saleStmt.setInt(3, sale.orderId)
            saleStmt.setString(4, sale.productName)
            saleStmt.setString(5, sale.category)
            saleStmt.setInt(6, sale.quantity)
            saleStmt.setDouble(7, sale.price)
            saleStmt.setString(8, sale.orderDate)
            saleStmt.setString(9, productFirebaseId)
            saleStmt.setString(10, sale.paymentMode)
            saleStmt.setString(11, sale.gcashReferenceId)
            saleStmt.setString(12, UserSession.getCurrentUser()?.username ?: "system")
            saleStmt.executeUpdate()
            saleStmt.close()

            android.util.Log.d("PostgreSQL", "✅ Sale recorded")

            // Step 2: Get recipe ingredients
            val getIngredientsQuery = """
                SELECT ri.ingredient_firebase_id, ri.ingredient_name,
                       ri.quantity_needed, ri.unit, p.firebase_id
                FROM recipes r
                JOIN recipe_ingredients ri ON r.id = ri.recipe_firebase_id
                JOIN products p ON ri.ingredient_firebase_id = p.id
                WHERE r.product_firebase_id = (SELECT id FROM products WHERE firebase_id = ? LIMIT 1)
            """
            val ingredientsStmt = connection.prepareStatement(getIngredientsQuery)
            ingredientsStmt.setString(1, productFirebaseId)
            val ingredientsRs = ingredientsStmt.executeQuery()

            val ingredients = mutableListOf<Ingredient>()
            while (ingredientsRs.next()) {
                ingredients.add(
                    Ingredient(
                        firebaseId = ingredientsRs.getString("firebase_id"),
                        name = ingredientsRs.getString("ingredient_name"),
                        quantityNeeded = ingredientsRs.getDouble("quantity_needed"),
                        unit = ingredientsRs.getString("unit")
                    )
                )
            }
            ingredientsRs.close()
            ingredientsStmt.close()

            android.util.Log.d("PostgreSQL", "📋 Found ${ingredients.size} ingredients in recipe")

            if (ingredients.isEmpty()) {
                android.util.Log.w("PostgreSQL", "⚠️ No recipe found for product - no ingredients to deduct")
            }

            // Step 3: Deduct each ingredient
            for (ingredient in ingredients) {
                val totalQuantityNeeded = ingredient.quantityNeeded * quantitySold

                android.util.Log.d("PostgreSQL", "📦 Deducting ingredient: ${ingredient.name}")
                android.util.Log.d("PostgreSQL", "   Amount needed: $totalQuantityNeeded ${ingredient.unit}")

                // Get current inventory
                val getInventoryQuery = """
                    SELECT inventory_a, inventory_b, quantity FROM products
                    WHERE firebase_id = ?
                """
                val invStmt = connection.prepareStatement(getInventoryQuery)
                invStmt.setString(1, ingredient.firebaseId)
                val invRs = invStmt.executeQuery()

                if (!invRs.next()) {
                    android.util.Log.w("PostgreSQL", "⚠️ Ingredient not found: ${ingredient.name}")
                    continue
                }

                var inventoryA = invRs.getInt("inventory_a")
                var inventoryB = invRs.getInt("inventory_b")
                invRs.close()
                invStmt.close()

                android.util.Log.d("PostgreSQL", "   Before - A: $inventoryA, B: $inventoryB")

                // Deduct from B first, then A
                var remaining = totalQuantityNeeded.toInt()
                if (inventoryB > 0) {
                    val deductFromB = minOf(remaining, inventoryB)
                    inventoryB -= deductFromB
                    remaining -= deductFromB
                    android.util.Log.d("PostgreSQL", "   Deducted $deductFromB from Inventory B")
                }

                if (remaining > 0 && inventoryA > 0) {
                    val deductFromA = minOf(remaining, inventoryA)
                    inventoryA -= deductFromA
                    remaining -= deductFromA
                    android.util.Log.d("PostgreSQL", "   Deducted $deductFromA from Inventory A")
                }

                val newQuantity = inventoryA + inventoryB

                android.util.Log.d("PostgreSQL", "   After - A: $inventoryA, B: $inventoryB, Total: $newQuantity")

                if (remaining > 0) {
                    android.util.Log.w("PostgreSQL", "⚠️ Insufficient stock for ${ingredient.name}! Short by: $remaining")
                }

                // Update inventory
                val updateQuery = """
                    UPDATE products
                    SET inventory_a = ?, inventory_b = ?, quantity = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE firebase_id = ?
                """
                val updateStmt = connection.prepareStatement(updateQuery)
                updateStmt.setInt(1, inventoryA)
                updateStmt.setInt(2, inventoryB)
                updateStmt.setInt(3, newQuantity)
                updateStmt.setString(4, ingredient.firebaseId)
                updateStmt.executeUpdate()
                updateStmt.close()
            }

            connection.commit()
            android.util.Log.d("PostgreSQL", "✅ Transaction committed successfully")
            android.util.Log.d("PostgreSQL", "━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success("Sale processed - ingredients deducted")

        } catch (e: Exception) {
            connection?.rollback()
            android.util.Log.e("PostgreSQL", "❌ Error processing sale: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection?.autoCommit = true
            connection?.close()
        }
    }

    // ========================================================================
    // TRANSFER INVENTORY (A → B)
    // ========================================================================

    /**
     * Transfer inventory from warehouse (A) to display (B)
     */
    suspend fun transferInventory(productFirebaseId: String, quantity: Int): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            connection = getConnection()

            // Get current inventory
            val getQuery = "SELECT inventory_a, inventory_b FROM products WHERE firebase_id = ?"
            val getStmt = connection.prepareStatement(getQuery)
            getStmt.setString(1, productFirebaseId)
            val rs = getStmt.executeQuery()

            if (!rs.next()) {
                return@withContext Result.failure(Exception("Product not found"))
            }

            val inventoryA = rs.getInt("inventory_a")
            val inventoryB = rs.getInt("inventory_b")
            rs.close()
            getStmt.close()

            if (inventoryA < quantity) {
                return@withContext Result.failure(Exception("Insufficient stock in Inventory A"))
            }

            // Transfer
            val updateQuery = """
                UPDATE products
                SET inventory_a = ?, inventory_b = ?, updated_at = CURRENT_TIMESTAMP
                WHERE firebase_id = ?
            """
            val updateStmt = connection.prepareStatement(updateQuery)
            updateStmt.setInt(1, inventoryA - quantity)
            updateStmt.setInt(2, inventoryB + quantity)
            updateStmt.setString(3, productFirebaseId)
            updateStmt.executeUpdate()
            updateStmt.close()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.close()
        }
    }

    // ========================================================================
    // HELPER FUNCTIONS
    // ========================================================================

    private fun ResultSet.toProduct(): Entity_Products {
        return Entity_Products(
            id = 0,  // Room auto-generates
            firebaseId = getString("firebase_id") ?: "",
            name = getString("name") ?: "",
            category = getString("category") ?: "",
            price = getDouble("price"),
            quantity = getInt("quantity"),
            inventoryA = getInt("inventory_a"),
            inventoryB = getInt("inventory_b"),
            costPerUnit = getDouble("cost_per_unit"),
            imageUri = getString("image_uri") ?: ""
        )
    }

    // Data class for ingredient
    private data class Ingredient(
        val firebaseId: String,
        val name: String,
        val quantityNeeded: Double,
        val unit: String
    )

    /**
     * Test connection to PostgreSQL
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = getConnection()
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM products")
            resultSet.next()
            val count = resultSet.getInt("count")
            resultSet.close()
            statement.close()
            connection.close()

            Result.success("✅ Connected to PostgreSQL! Found $count products.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
