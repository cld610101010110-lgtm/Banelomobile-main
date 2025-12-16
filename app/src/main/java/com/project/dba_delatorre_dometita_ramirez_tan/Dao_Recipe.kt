package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.*

@Dao
interface Dao_Recipe {
    // READ OPERATIONS

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipes(): List<Entity_Recipe>

    @Transaction
    @Query("SELECT * FROM recipes WHERE productId = :productId LIMIT 1")
    suspend fun getRecipeWithIngredients(productId: String): RecipeWithIngredients?

    @Transaction
    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipesWithIngredients(): List<RecipeWithIngredients>

    @Query("""SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId""")
    suspend fun getIngredientsByRecipeId(recipeId: Int): List<Entity_RecipeIngredient>

    //WRITE OPERATION

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Entity_Recipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Entity_RecipeIngredient)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecipes(recipes: List<Entity_Recipe>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIngredients(ingredients: List<Entity_RecipeIngredient>)

    @Query("DELETE FROM recipes WHERE firebaseId = :firebaseId")
    suspend fun deleteRecipeByFirebaseId(firebaseId: String)

    @Query("DELETE FROM recipe_ingredients WHERE firebaseId = :firebaseId")
    suspend fun deleteIngredientByFirebaseId(firebaseId: String)

    @Query("DELETE FROM recipes")
    suspend fun clearAllRecipes()

    @Query("DELETE FROM recipe_ingredients")
    suspend fun clearAllIngredients()


    @Query("SELECT * FROM recipes WHERE productId = :productId LIMIT 1")
    suspend fun getRecipeByProductId(productId: String): Entity_Recipe?

    @Query("SELECT * FROM recipes WHERE firebaseId = :recipeFirebaseId LIMIT 1")
    suspend fun getRecipeByFirebaseId(recipeFirebaseId: String): Entity_Recipe?


}