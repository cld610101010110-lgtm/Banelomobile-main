package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface Dao_Products {
    @Insert
    suspend fun insertProduct(product: Entity_Products)

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Entity_Products>

    @Query("SELECT * FROM products WHERE category = :category")
    suspend fun getByCategory(category: String): List<Entity_Products>

    @Delete
    suspend fun deleteProduct(product: Entity_Products)

    @Update
    suspend fun updateProduct(product: Entity_Products)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProducts(products: List<Entity_Products>)

    // ✅ ADD THIS METHOD to clear old data
    @Query("DELETE FROM products")
    fun deleteAllProducts()

    // ✅ ADD THIS NEW QUERY
    @Query("SELECT * FROM products WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getProductByFirebaseId(firebaseId: String): Entity_Products?

}
