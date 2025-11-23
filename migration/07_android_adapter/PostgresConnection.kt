package com.example.banelo.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.SQLException

/**
 * PostgreSQL Connection Manager for Android
 *
 * NOTE: Direct PostgreSQL connections from Android are NOT recommended for production.
 * This is provided as a reference implementation for migration testing.
 *
 * Production recommendation: Use a REST API backend (Node.js, Spring Boot, etc.)
 * that connects to PostgreSQL, and access it via Retrofit/OkHttp from Android.
 */
object PostgresConnection {

    // PostgreSQL connection details
    // IMPORTANT: Store these in BuildConfig or environment variables, NOT in code!
    // For testing: Use your computer's local IP (not localhost from Android)
    // Find your IP: Run 'ipconfig' in Windows Command Prompt, look for IPv4 Address
    private const val DB_HOST = "10.0.2.2"  // Android emulator special IP for host machine
    // Or use your actual IP like "192.168.1.100" for physical devices
    private const val DB_PORT = "5432"
    private const val DB_NAME = "banelo_db"
    private const val DB_USER = "postgres"
    private const val DB_PASSWORD = "admin123"

    private const val JDBC_URL = "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"

    /**
     * Get PostgreSQL connection
     * Must be called from a background thread (coroutine)
     */
    suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        try {
            // Load PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver")

            // Create connection
            DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)
        } catch (e: ClassNotFoundException) {
            throw Exception("PostgreSQL JDBC driver not found. Add to build.gradle: implementation 'org.postgresql:postgresql:42.6.0'")
        } catch (e: SQLException) {
            throw Exception("Failed to connect to PostgreSQL: ${e.message}")
        }
    }

    /**
     * Execute a query and return ResultSet
     */
    suspend fun executeQuery(query: String, params: List<Any> = emptyList()): ResultSet = withContext(Dispatchers.IO) {
        val connection = getConnection()
        val statement = connection.prepareStatement(query)

        // Bind parameters
        params.forEachIndexed { index, param ->
            statement.setObject(index + 1, param)
        }

        statement.executeQuery()
    }

    /**
     * Execute an update/insert/delete query
     * Returns number of affected rows
     */
    suspend fun executeUpdate(query: String, params: List<Any> = emptyList()): Int = withContext(Dispatchers.IO) {
        val connection = getConnection()
        val statement = connection.prepareStatement(query)

        // Bind parameters
        params.forEachIndexed { index, param ->
            statement.setObject(index + 1, param)
        }

        statement.executeUpdate()
    }

    /**
     * Close connection
     */
    fun closeConnection(connection: Connection?) {
        try {
            connection?.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}

/**
 * Example usage in a Repository:
 *
 * class ProductRepository {
 *     suspend fun getAllProducts(): List<Entity_Products> {
 *         return withContext(Dispatchers.IO) {
 *             val connection = PostgresConnection.getConnection()
 *             val resultSet = PostgresConnection.executeQuery(
 *                 "SELECT * FROM products WHERE is_active = ?",
 *                 listOf(true)
 *             )
 *
 *             val products = mutableListOf<Entity_Products>()
 *             while (resultSet.next()) {
 *                 products.add(
 *                     Entity_Products(
 *                         firebaseId = resultSet.getString("firebase_id"),
 *                         name = resultSet.getString("name"),
 *                         category = resultSet.getString("category"),
 *                         price = resultSet.getDouble("price"),
 *                         quantity = resultSet.getInt("quantity"),
 *                         inventoryA = resultSet.getInt("inventory_a"),
 *                         inventoryB = resultSet.getInt("inventory_b"),
 *                         costPerUnit = resultSet.getDouble("cost_per_unit"),
 *                         imageUri = resultSet.getString("image_uri")
 *                     )
 *                 )
 *             }
 *
 *             PostgresConnection.closeConnection(connection)
 *             products
 *         }
 *     }
 * }
 */
