package com.project.dba_delatorre_dometita_ramirez_tan

class RepositoryUsers(private val daoUsers: Dao_Users) {
    val allUsers = daoUsers.DaoLoadUsers()

    suspend fun repInsert(users: Entity_Users) = daoUsers.DaoInsert(users)
    suspend fun repUpdate(users: Entity_Users) = daoUsers.DaoUpdate(users)
    suspend fun repDelete(users: Entity_Users) = daoUsers.DaoDelete(users)
}
