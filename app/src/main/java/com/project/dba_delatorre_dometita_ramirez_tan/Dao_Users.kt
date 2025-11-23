package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface Dao_Users {
    @Insert
    suspend fun DaoInsert(users: Entity_Users)

    @Update
    suspend fun DaoUpdate(users: Entity_Users)

    @Delete
    suspend fun DaoDelete(users: Entity_Users)

    @Query("SELECT * FROM tbl_Users ORDER BY Entity_id ASC")
    fun DaoLoadUsers(): Flow<List<Entity_Users>>
    // ✅ Get user by username only (no password check)
    @Query("SELECT * FROM tbl_Users WHERE Entity_username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): Entity_Users?

    // ✅ Get active user by username
    @Query("SELECT * FROM tbl_Users WHERE Entity_username = :username AND status = 'active' LIMIT 1")
    suspend fun getActiveUserByUsername(username: String): Entity_Users?
}