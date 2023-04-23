package com.touchsi.buddhawajana.vm

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchsi.buddhawajana.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BaseViewModel<E, J>(private val repo: Repository<E, J>): ViewModel() {
    private val _items: Flow<List<E>> = repo.getItems(0)

    open val isRefreshing = MutableStateFlow(false)

    fun getItems(): Flow<List<E>> = _items

    fun update(entity: E) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.update(entity)
        }
    }

    open fun refresh(owner: LifecycleOwner) {
        viewModelScope.launch {
            isRefreshing.emit(true)
            viewModelScope.launch(Dispatchers.IO) {
                repo.refreshFromServer(owner) {
                    viewModelScope.launch {
                        isRefreshing.emit(false)
                    }
                }
            }
        }
    }
}