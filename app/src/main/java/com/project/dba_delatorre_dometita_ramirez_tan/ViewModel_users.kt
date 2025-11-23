package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ViewModel_users(private val rep: RepositoryUsers): ViewModel() {
    var userToEdit : Entity_Users? by mutableStateOf(null)
    val users = rep.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), emptyList())

    fun viewModel_insert(users: Entity_Users) = viewModelScope.launch {
        rep.repInsert(users)
    }

    fun viewModel_update(users: Entity_Users) = viewModelScope.launch {
        rep.repUpdate(users)
    }

    fun viewModel_delete(users: Entity_Users) = viewModelScope.launch {
        rep.repDelete(users)
    }

    fun viewModel_userToEdit(user: Entity_Users?){
        userToEdit = user

    }

}

class viewModel_Factory(private val repo: RepositoryUsers): ViewModelProvider.Factory{
    override fun <T: ViewModel> create(modelClass: Class<T>):T{
        return ViewModel_users(repo) as T
    }
}