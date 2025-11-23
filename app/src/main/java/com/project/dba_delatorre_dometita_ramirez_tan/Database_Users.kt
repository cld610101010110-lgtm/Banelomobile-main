package com.project.dba_delatorre_dometita_ramirez_tan

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Entity_Users::class], version = 4)  // ✅ Changed to version 4
abstract class Database_Users : RoomDatabase() {
    abstract fun dao_users(): Dao_Users

    companion object {
        @Volatile
        private var INSTANCE: Database_Users? = null

        // Migration from version 1 to 2 (added profileImageUri)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tbl_Users ADD COLUMN profileImageUri TEXT")
            }
        }

        // Migration from version 2 to 3 (remove password, add role/status/joinedDate)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE tbl_Users_new (
                        Entity_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        Entity_lname TEXT NOT NULL,
                        Entity_fname TEXT NOT NULL,
                        Entity_mname TEXT NOT NULL,
                        Entity_username TEXT NOT NULL,
                        profileImageUri TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'Staff',
                        status TEXT NOT NULL DEFAULT 'active',
                        joinedDate TEXT NOT NULL DEFAULT ''
                    )
                """)

                database.execSQL("""
                    INSERT INTO tbl_Users_new (Entity_id, Entity_lname, Entity_fname, Entity_mname, Entity_username, profileImageUri, role, status, joinedDate)
                    SELECT Entity_id, Entity_lname, Entity_fname, Entity_mname, Entity_username, 
                           COALESCE(profileImageUri, ''), 'Staff', 'active', ''
                    FROM tbl_Users
                """)

                database.execSQL("DROP TABLE tbl_Users")
                database.execSQL("ALTER TABLE tbl_Users_new RENAME TO tbl_Users")
            }
        }

        // Migration from version 3 to 4 (remove profileImageUri)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table without profileImageUri
                database.execSQL("""
                    CREATE TABLE tbl_Users_new (
                        Entity_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        Entity_lname TEXT NOT NULL,
                        Entity_fname TEXT NOT NULL,
                        Entity_mname TEXT NOT NULL,
                        Entity_username TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'Staff',
                        status TEXT NOT NULL DEFAULT 'active',
                        joinedDate TEXT NOT NULL DEFAULT ''
                    )
                """)

                // Copy data (excluding profileImageUri)
                database.execSQL("""
                    INSERT INTO tbl_Users_new (Entity_id, Entity_lname, Entity_fname, Entity_mname, Entity_username, role, status, joinedDate)
                    SELECT Entity_id, Entity_lname, Entity_fname, Entity_mname, Entity_username, role, status, joinedDate
                    FROM tbl_Users
                """)

                // Drop old table
                database.execSQL("DROP TABLE tbl_Users")

                // Rename new table
                database.execSQL("ALTER TABLE tbl_Users_new RENAME TO tbl_Users")
            }
        }

        fun getDatabase(context: Context): Database_Users {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Database_Users::class.java,
                    "db_users"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)  // ✅ Add new migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}