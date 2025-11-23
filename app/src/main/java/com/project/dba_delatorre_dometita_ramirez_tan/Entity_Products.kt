package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Entity_Products(
    val id: Int = 0,
    @PrimaryKey val firebaseId: String = "",
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int, // Computed total: inventoryA + inventoryB (for backward compatibility)
    val inventoryA: Int = 0, // Main/Warehouse inventory
    val inventoryB: Int = 0, // Expendable/Display inventory (deducted first)
    val costPerUnit: Double = 0.0, // Cost per unit for waste calculation (e.g., ₱0.20 per gram)
    val imageUri: String
)