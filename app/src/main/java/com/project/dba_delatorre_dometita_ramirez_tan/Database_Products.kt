package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Entity_Products::class,
        Entity_SalesReport::class,
        Entity_Recipe::class,
        Entity_RecipeIngredient::class,
        Entity_AuditLog::class,
        Entity_WasteLog::class
    ],
    version = 17,
    exportSchema = false
)
abstract class Database_Products : RoomDatabase() {
    abstract fun dao_products(): Dao_Products
    abstract fun dao_salesReport(): Dao_SalesReport
    abstract fun daoRecipe(): Dao_Recipe
    abstract fun daoAuditLog(): Dao_AuditLog
    abstract fun daoWasteLog(): Dao_WasteLog

    companion object {
        @Volatile
        private var INSTANCE: Database_Products? = null

        fun getDatabase(context: Context): Database_Products {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Database_Products::class.java,
                    "products_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}