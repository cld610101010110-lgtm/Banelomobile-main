package com.project.dba_delatorre_dometita_ramirez_tan

object UserSession {
    var currentUser: Entity_Users? = null

    fun isLoggedIn(): Boolean = currentUser != null

    fun getUserFullName(): String {
        return currentUser?.let {
            "${it.Entity_fname} ${it.Entity_lname}"
        } ?: "Guest"
    }

    fun logout() {
        currentUser = null
    }
}