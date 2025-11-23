package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waste_logs")
data class Entity_WasteLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseId: String = "", // For syncing with Firestore
    val productFirebaseId: String, // Reference to the product
    val productName: String,
    val category: String,
    val quantity: Int, // Amount wasted
    val reason: String = "End of day waste", // Reason for waste
    val wasteDate: String, // Format: "yyyy-MM-dd HH:mm:ss"
    val recordedBy: String, // Username who recorded the waste
    val isSyncedToFirebase: Boolean = false // Track sync status
)
