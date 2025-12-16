package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Entity_Recipe(
    @PrimaryKey(autoGenerate = true)
    val recipeId: Int = 0,

    val firebaseId: String? = "",

    val productId: String? = "",                    // ✅ UUID id of the product (replaces productFirebaseId)

    val productName: String? = ""
)