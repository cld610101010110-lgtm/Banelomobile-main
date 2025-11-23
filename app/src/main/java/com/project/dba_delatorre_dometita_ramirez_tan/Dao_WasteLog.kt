package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface Dao_WasteLog {

    @Insert
    suspend fun insertWasteLog(wasteLog: Entity_WasteLog)

    @Insert
    suspend fun insertWasteLogs(wasteLogs: List<Entity_WasteLog>)

    @Query("SELECT * FROM waste_logs ORDER BY wasteDate DESC")
    suspend fun getAllWasteLogs(): List<Entity_WasteLog>

    @Query("SELECT * FROM waste_logs WHERE productFirebaseId = :productId ORDER BY wasteDate DESC")
    suspend fun getWasteLogsByProduct(productId: String): List<Entity_WasteLog>

    @Query("SELECT * FROM waste_logs WHERE wasteDate BETWEEN :startDate AND :endDate ORDER BY wasteDate DESC")
    suspend fun getWasteLogsByDateRange(startDate: String, endDate: String): List<Entity_WasteLog>

    @Query("SELECT * FROM waste_logs WHERE recordedBy = :username ORDER BY wasteDate DESC")
    suspend fun getWasteLogsByUser(username: String): List<Entity_WasteLog>

    @Query("SELECT * FROM waste_logs WHERE isSyncedToFirebase = 0")
    suspend fun getUnsyncedWasteLogs(): List<Entity_WasteLog>

    @Query("UPDATE waste_logs SET isSyncedToFirebase = 1, firebaseId = :firebaseId WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, firebaseId: String)

    @Query("DELETE FROM waste_logs")
    suspend fun clearAllWasteLogs()

    @Query("SELECT SUM(quantity) FROM waste_logs WHERE productFirebaseId = :productId")
    suspend fun getTotalWasteForProduct(productId: String): Int?

    @Query("SELECT SUM(quantity) FROM waste_logs WHERE wasteDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalWasteByDateRange(startDate: String, endDate: String): Int?
}
