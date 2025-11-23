package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class Entity_AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseId: String = "",
    val username: String,                    // Who did the action
    val action: String,                      // What action (LOGIN, SALE_TRANSACTION, etc.)
    val description: String,                 // Details of the action
//    val timestamp: Long = System.currentTimeMillis(),
    val dateTime: String,                    // Formatted date time
    val status: String = "Success",           // Success or Failed
    val isOnline: Boolean = false
)

// Action types for consistency
object AuditActions {
    const val LOGIN = "LOGIN"
    const val LOGOUT = "LOGOUT"
    const val FAILED_LOGIN = "FAILED_LOGIN"
    const val SALE_TRANSACTION = "SALE_TRANSACTION"
    const val INVENTORY_UPDATE = "INVENTORY_UPDATE"
    const val PRODUCT_ADD = "PRODUCT_ADD"
    const val PRODUCT_EDIT = "PRODUCT_EDIT"
    const val PRODUCT_DELETE = "PRODUCT_DELETE"
    const val WASTE_MARKED = "WASTE_MARKED"
//    const val PRICE_UPDATE = "PRICE_UPDATE"
//    const val RECIPE_ADD = "RECIPE_ADD"
//    const val RECIPE_UPDATE = "RECIPE_UPDATE"
//    const val USER_MANAGEMENT = "USER_MANAGEMENT"
//    const val SYSTEM_SETTINGS = "SYSTEM_SETTINGS"
}

