package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Entity_Products(
    @PrimaryKey val id: String = "",               // ✅ UUID String from PostgreSQL (primary key)
    val firebaseId: String = "",                   // Legacy field - kept for backward compatibility but not used
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int, // Computed total: inventoryA + inventoryB (for backward compatibility)
    val inventoryA: Int = 0, // Main/Warehouse inventory
    val inventoryB: Int = 0, // Expendable/Display inventory (deducted first)
    val costPerUnit: Double = 0.0, // Cost per unit for waste calculation (e.g., ₱0.20 per gram)
    val image_uri: String? = null,

    val isPerishable: Boolean = false,           // Only for ingredients
    val shelfLifeDays: Int = 0,                  // How many days until expiration
    val expirationDate: String? = null,          // ISO format: "2025-01-20" (for Inventory B only)
    val transferredToB: Boolean = false          // Track if currently in Inventory B
)