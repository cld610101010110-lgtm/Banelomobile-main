package com.example.banelo.database

import com.example.banelo.Entity_Products
import com.example.banelo.Entity_SalesReport
import com.example.banelo.Entity_WasteLog
import com.example.banelo.Entity_Recipe
import com.example.banelo.Entity_RecipeIngredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.ResultSet
import java.util.UUID

/**
 * Database Adapter: Firestore-style API for PostgreSQL
 *
 * This adapter provides a Firestore-like interface for PostgreSQL,
 * making the migration easier by minimizing code changes.
 *
 * Usage:
 * Replace: FirebaseFirestore.getInstance()
 * With:    DatabaseAdapter
 *
 * Example:
 * OLD: firestore.collection("products").get()
 * NEW: DatabaseAdapter.getProducts()
 */
object DatabaseAdapter {

    // ========================================================================
    // PRODUCTS
    // ========================================================================

    /**
     * Get all products
     * Equivalent to: firestore.collection("products").get()
     */
    suspend fun getProducts(): List<Entity_Products> = withContext(Dispatchers.IO) {
        val query = """
            SELECT id, firebase_id, name, category, price, quantity,
                   inventory_a, inventory_b, cost_per_unit, image_uri
            FROM products
            WHERE is_active = true
            ORDER BY name
        """

        val resultSet = PostgresConnection.executeQuery(query)
        val products = mutableListOf<Entity_Products>()

        while (resultSet.next()) {
            products.add(resultSet.toProduct())
        }

        products
    }

    /**
     * Get product by ID
     */
    suspend fun getProductById(id: String): Entity_Products? = withContext(Dispatchers.IO) {
        val query = """
            SELECT id, firebase_id, name, category, price, quantity,
                   inventory_a, inventory_b, cost_per_unit, image_uri
            FROM products
            WHERE firebase_id = ?
        """

        val resultSet = PostgresConnection.executeQuery(query, listOf(id))

        if (resultSet.next()) {
            resultSet.toProduct()
        } else {
            null
        }
    }

    /**
     * Get products by category
     * Equivalent to: firestore.collection("products").whereEqualTo("category", category).get()
     */
    suspend fun getProductsByCategory(category: String): List<Entity_Products> = withContext(Dispatchers.IO) {
        val query = """
            SELECT id, firebase_id, name, category, price, quantity,
                   inventory_a, inventory_b, cost_per_unit, image_uri
            FROM products
            WHERE category = ? AND is_active = true
            ORDER BY name
        """

        val resultSet = PostgresConnection.executeQuery(query, listOf(category))
        val products = mutableListOf<Entity_Products>()

        while (resultSet.next()) {
            products.add(resultSet.toProduct())
        }

        products
    }

    /**
     * Add new product
     * Equivalent to: firestore.collection("products").add(product)
     */
    suspend fun addProduct(product: Entity_Products): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()

        val query = """
            INSERT INTO products (id, firebase_id, name, category, price, quantity,
                                 inventory_a, inventory_b, cost_per_unit, image_uri)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        PostgresConnection.executeUpdate(
            query,
            listOf(
                id,
                product.firebaseId,
                product.name,
                product.category,
                product.price,
                product.quantity,
                product.inventoryA,
                product.inventoryB,
                product.costPerUnit,
                product.imageUri
            )
        )

        id
    }

    /**
     * Update product
     * Equivalent to: firestore.collection("products").document(id).update(updates)
     */
    suspend fun updateProduct(firebaseId: String, product: Entity_Products): Boolean = withContext(Dispatchers.IO) {
        val query = """
            UPDATE products
            SET name = ?, category = ?, price = ?, quantity = ?,
                inventory_a = ?, inventory_b = ?, cost_per_unit = ?, image_uri = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE firebase_id = ?
        """

        val rowsUpdated = PostgresConnection.executeUpdate(
            query,
            listOf(
                product.name,
                product.category,
                product.price,
                product.quantity,
                product.inventoryA,
                product.inventoryB,
                product.costPerUnit,
                product.imageUri,
                firebaseId
            )
        )

        rowsUpdated > 0
    }

    /**
     * Delete product
     * Equivalent to: firestore.collection("products").document(id).delete()
     */
    suspend fun deleteProduct(firebaseId: String): Boolean = withContext(Dispatchers.IO) {
        val query = "UPDATE products SET is_active = false WHERE firebase_id = ?"
        val rowsUpdated = PostgresConnection.executeUpdate(query, listOf(firebaseId))
        rowsUpdated > 0
    }

    // ========================================================================
    // SALES
    // ========================================================================

    /**
     * Add sale transaction
     * Equivalent to: firestore.collection("sales").add(sale)
     */
    suspend fun addSale(sale: Entity_SalesReport): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()

        val query = """
            INSERT INTO sales (id, firebase_id, product_name, category, quantity, price,
                              order_date, product_firebase_id, payment_mode, gcash_reference_id, cashier_username)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        PostgresConnection.executeUpdate(
            query,
            listOf(
                id,
                UUID.randomUUID().toString(),
                sale.productName,
                sale.category,
                sale.quantity,
                sale.price,
                sale.orderDate,
                sale.productFirebaseId,
                sale.paymentMode,
                sale.gcashReferenceId,
                "current_user"  // Replace with actual username from session
            )
        )

        id
    }

    /**
     * Get sales by date range
     */
    suspend fun getSalesByDateRange(startDate: String, endDate: String): List<Entity_SalesReport> = withContext(Dispatchers.IO) {
        val query = """
            SELECT id, firebase_id, order_id, product_name, category, quantity, price,
                   order_date, product_firebase_id, payment_mode, gcash_reference_id
            FROM sales
            WHERE order_date BETWEEN ? AND ?
            ORDER BY order_date DESC
        """

        val resultSet = PostgresConnection.executeQuery(query, listOf(startDate, endDate))
        val sales = mutableListOf<Entity_SalesReport>()

        while (resultSet.next()) {
            sales.add(resultSet.toSale())
        }

        sales
    }

    // ========================================================================
    // WASTE LOGS
    // ========================================================================

    /**
     * Add waste log
     * Equivalent to: firestore.collection("waste_logs").add(wasteLog)
     */
    suspend fun addWasteLog(wasteLog: Entity_WasteLog): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()

        val query = """
            INSERT INTO waste_logs (id, firebase_id, product_firebase_id, product_name, category,
                                   quantity, reason, waste_date, recorded_by, cost_impact)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        PostgresConnection.executeUpdate(
            query,
            listOf(
                id,
                wasteLog.firebaseId,
                wasteLog.productFirebaseId,
                wasteLog.productName,
                wasteLog.category,
                wasteLog.quantity,
                wasteLog.reason,
                wasteLog.wasteDate,
                wasteLog.recordedBy,
                0.0  // Calculate cost impact from product
            )
        )

        id
    }

    // ========================================================================
    // HELPER EXTENSION FUNCTIONS
    // ========================================================================

    private fun ResultSet.toProduct(): Entity_Products {
        return Entity_Products(
            id = getInt("id"),
            firebaseId = getString("firebase_id"),
            name = getString("name"),
            category = getString("category"),
            price = getDouble("price"),
            quantity = getInt("quantity"),
            inventoryA = getInt("inventory_a"),
            inventoryB = getInt("inventory_b"),
            costPerUnit = getDouble("cost_per_unit"),
            imageUri = getString("image_uri") ?: ""
        )
    }

    private fun ResultSet.toSale(): Entity_SalesReport {
        return Entity_SalesReport(
            orderId = getInt("order_id"),
            productName = getString("product_name"),
            category = getString("category"),
            quantity = getInt("quantity"),
            price = getDouble("price"),
            orderDate = getString("order_date"),
            productFirebaseId = getString("product_firebase_id"),
            paymentMode = getString("payment_mode"),
            gcashReferenceId = getString("gcash_reference_id") ?: ""
        )
    }
}

/**
 * Migration Example:
 *
 * BEFORE (Firestore):
 * ```kotlin
 * firestore.collection("products")
 *     .whereEqualTo("category", "Pastries")
 *     .get()
 *     .addOnSuccessListener { documents ->
 *         val products = documents.map { it.toObject<Entity_Products>() }
 *         // Use products
 *     }
 * ```
 *
 * AFTER (PostgreSQL):
 * ```kotlin
 * viewModelScope.launch {
 *     val products = DatabaseAdapter.getProductsByCategory("Pastries")
 *     // Use products
 * }
 * ```
 */
