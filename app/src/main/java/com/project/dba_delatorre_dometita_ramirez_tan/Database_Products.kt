package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Entity_Products::class,
        Entity_SalesReport::class,
        Entity_Recipe::class,
        Entity_RecipeIngredient::class,
        Entity_AuditLog::class,
        Entity_WasteLog::class
    ],
    version = 34,
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

        // ✅ NEW MIGRATION
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN isPerishable INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE products ADD COLUMN shelfLifeDays INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE products ADD COLUMN expirationDate TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN transferredToB INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): Database_Products {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Database_Products::class.java,
                    "products_database"
                )
                    .addMigrations(MIGRATION_20_21)  // ✅ NEW
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}