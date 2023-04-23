package com.touchsi.buddhawajana.repository

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class Repository<E,J> {

    abstract fun getItems(id: Long): Flow<List<E>>
    abstract fun getItemsFromService(): Observable<List<J>>
    abstract fun getEntityId(entity: E): Long
    abstract fun getJsonId(json: J): Long

    abstract fun shouldBeUpdated(entity: E, json: J): Boolean
    abstract fun convertJsonToEntity(json: J): E

    abstract suspend fun get(id: Long): E
    abstract suspend fun insert(entity: E)
    abstract suspend fun delete(entity: E)
    abstract suspend fun update(entity: E)

    open fun skipDeleting(): Boolean = false
    open fun skipDeleteEntity(entity: E) = false

    fun refreshFromServer(owner: LifecycleOwner,
                          onComplete:() -> Unit): Disposable {
        return getItemsFromService()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->
                            owner.lifecycleScope.launch {
                                updateItems(result)
                            }
                        },
                        { error ->
                            error.message?.let { Log.d("error", it) }
                        },
                        { onComplete.invoke() }
                )
    }

    suspend fun updateItems(result: List<J>, id: Long = 0) {
        getItems(id).collectLatest { entities ->
            result.forEach {
                val entity = entities.singleOrNull { entity ->
                    getEntityId(entity) == getJsonId(it)
                }
                if (entity == null) {
                    withContext(Dispatchers.IO) {
                        insert(convertJsonToEntity(it))
                    }
                } else if (shouldBeUpdated(entity, it)) {
                    withContext(Dispatchers.IO) {
                        update(convertJsonToEntity(it))
                    }
                }
            }
            if (!skipDeleting()) {
                entities.forEach {
                    val json = result.singleOrNull { json -> getJsonId(json) == getEntityId(it) }
                    if (json == null && !skipDeleteEntity(it)) {
                        withContext(Dispatchers.IO) {
                            delete(it)
                        }
                    }
                }
            }
        }
    }

}