package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart

object RoleManager {

    // Role constants
    const val ROLE_MANAGER = "Manager"
    const val ROLE_CASHIER = "Cashier"
    const val ROLE_INVENTORY_CLERK = "Inventory Clerk"

    // Get current user role
    fun getCurrentUserRole(): String {
        return UserSession.currentUser?.role ?: ""
    }

    // Check if user is manager
    fun isManager(): Boolean {
        return getCurrentUserRole().equals(ROLE_MANAGER, ignoreCase = true)
    }

    // Check if user is cashier0
    fun isCashier(): Boolean {
        return getCurrentUserRole().equals(ROLE_CASHIER, ignoreCase = true)
    }

    // Check if user is inventory clerk
    fun isInventoryClerk(): Boolean {
        return getCurrentUserRole().equals(ROLE_INVENTORY_CLERK, ignoreCase = true)
    }

    // Check if user can access a specific route
    fun canAccessRoute(route: String): Boolean {
        val role = getCurrentUserRole()

        android.util.Log.d("RoleManager", "🔐 Checking access for role: $role to route: $route")

        return when {
            // Manager can access everything
            isManager() -> true

            // Cashier can only access Order Process
            isCashier() -> {
                route == Routes.OrderProcess.routes || route == Routes.R_Login.routes
            }

            // Inventory Clerk can only access Inventory List
            isInventoryClerk() -> {
                route == Routes.R_InventoryList.routes ||
                        route == Routes.R_AddProduct.routes ||
                        route.startsWith("EditProductScreen/") ||
                        route == Routes.R_Login.routes
            }

            else -> false
        }
    }

    // Get default route based on role (after login)
    fun getDefaultRoute(): String {
        return when {
            isManager() -> Routes.R_DashboardScreen.routes
            isCashier() -> Routes.OrderProcess.routes
            isInventoryClerk() -> Routes.R_InventoryList.routes
            else -> Routes.R_Login.routes
        }
    }

    // Get menu items based on role
    fun getMenuItemsForRole(): List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>> {
        return when {
            isManager() -> listOf(
                "Overview" to androidx.compose.material.icons.Icons.Filled.Home,
                "Order Process" to androidx.compose.material.icons.Icons.Filled.ShoppingCart,
                "Inventory List" to androidx.compose.material.icons.Icons.Filled.Email,
                "Log Out" to androidx.compose.material.icons.Icons.AutoMirrored.Filled.ExitToApp
            )

            isCashier() -> listOf(
                "Order Process" to androidx.compose.material.icons.Icons.Filled.ShoppingCart,
                "Log Out" to androidx.compose.material.icons.Icons.AutoMirrored.Filled.ExitToApp
            )

            isInventoryClerk() -> listOf(
                "Inventory List" to androidx.compose.material.icons.Icons.Filled.Email,
                "Log Out" to androidx.compose.material.icons.Icons.AutoMirrored.Filled.ExitToApp
            )

            else -> emptyList()
        }
    }
}

