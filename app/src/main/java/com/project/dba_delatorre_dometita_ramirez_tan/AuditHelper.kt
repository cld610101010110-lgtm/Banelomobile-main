package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AuditHelper {
    private var auditRepository: AuditRepository? = null

    fun initialize(context: Context) {
        val database = Database_Products.getDatabase(context)
        auditRepository = AuditRepository(database.daoAuditLog())
    }


    fun logLogin(username: String, fullName: String) {
        log(AuditActions.LOGIN, "$fullName logged in successfully", "Success", username) // ✅ Add "Success" here
    }

    fun logLogout(username: String, fullName: String) {
        log(AuditActions.LOGOUT, "$fullName logged out", "Success", username) // ✅ Add "Success" here
    }

    fun logFailedLogin(username: String) {
        log(AuditActions.FAILED_LOGIN, "Failed login attempt for username: $username", "Failed", username)
    }

    fun logSale(productName: String, quantity: Int, total: Double) {
        val currentUser = UserSession.currentUser
        val username = currentUser?.Entity_username ?: "Unknown"
        val fullName = UserSession.getUserFullName()
        log(
            AuditActions.SALE_TRANSACTION,
            "$fullName completed sale of $productName (${quantity}x) - ₱$total",
            "Success",
            username
        )
    }



    fun logProductAdd(productName: String) {
        val currentUser = UserSession.currentUser
        val username = currentUser?.Entity_username ?: "Unknown"
        val fullName = UserSession.getUserFullName()
        log(
            AuditActions.PRODUCT_ADD,
            "$fullName added new product: $productName",
            "Success",
            username
        )
    }

    fun logProductEdit(productName: String) {
        val currentUser = UserSession.currentUser
        val username = currentUser?.Entity_username ?: "Unknown"
        val fullName = UserSession.getUserFullName()
        log(
            AuditActions.PRODUCT_EDIT,
            "$fullName updated product: $productName",
            "Success",
            username
        )
    }

    fun logProductDelete(productName: String) {
        val currentUser = UserSession.currentUser
        val username = currentUser?.Entity_username ?: "Unknown"
        val fullName = UserSession.getUserFullName()
        log(
            AuditActions.PRODUCT_DELETE,
            "$fullName deleted product: $productName",
            "Success",
            username
        )
    }

    fun logWaste(productName: String, quantity: Int) {
        val currentUser = UserSession.currentUser
        val username = currentUser?.Entity_username ?: "Unknown"
        val fullName = UserSession.getUserFullName()
        log(
            AuditActions.WASTE_MARKED,
            "$fullName marked $quantity units of $productName as waste",
            "Success",
            username
        )
    }

    // ✅ KEEP THIS - The correct log function with username parameter
    private fun log(action: String, description: String, status: String = "Success", username: String) {
        CoroutineScope(Dispatchers.IO).launch {
            auditRepository?.logAction(action, description, status, username)
        }
    }
}
