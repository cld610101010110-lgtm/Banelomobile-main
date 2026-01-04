package com.project.dba_delatorre_dometita_ramirez_tan

import kotlin.random.Random

// ✅ Shared utility to generate unique firebaseIds
object FirebaseIdUtil {
    fun generate(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..20).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
