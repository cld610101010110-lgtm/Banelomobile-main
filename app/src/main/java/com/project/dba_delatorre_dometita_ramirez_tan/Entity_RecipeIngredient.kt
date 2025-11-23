package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Entity_Recipe::class,
            parentColumns = ["recipeId"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Entity_RecipeIngredient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseId: String = "",
    val recipeId: Int,
    val ingredientFirebaseId: String,
    val ingredientName: String,
    val quantityNeeded: Double,
    val unit: String = "g"
)