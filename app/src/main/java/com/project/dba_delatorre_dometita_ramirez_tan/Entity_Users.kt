package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tbl_Users")
data class Entity_Users(
    @PrimaryKey(autoGenerate = true) val Entity_id: Int = 0,
    val Entity_lname: String = "",           // Maps to "lname" in Firebase
    val Entity_fname: String = "",           // Maps to "fname" in Firebase
    val Entity_mname: String = "",           // Maps to "mname" in Firebase
    val Entity_username: String = "",        // Maps to "username" in Firebase
    val role: String = "Staff",              // Maps to "role" in Firebase (manager/staff)
    val status: String = "active",           // Maps to "status" in Firebase (active/inactive)
    val joinedDate: String = ""
)