package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waste_logs")
data class Entity_WasteLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "firebaseId")
    val firebaseId: String = "", // For syncing with Firestore

    @ColumnInfo(name = "productFirebaseId")
    val productFirebaseId: String, // Reference to the product

    @ColumnInfo(name = "productName")
    val productName: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "quantity")
    val quantity: Int, // Amount wasted

    @ColumnInfo(name = "reason")
    val reason: String = "End of day waste", // Reason for waste

    @ColumnInfo(name = "wasteDate")
    val wasteDate: String, // Format: "yyyy-MM-dd HH:mm:ss"

    @ColumnInfo(name = "recordedBy")
    val recordedBy: String, // Username who recorded the waste

    @ColumnInfo(name = "isSyncedToFirebase")
    val isSyncedToFirebase: Boolean = false // Track sync status
)
