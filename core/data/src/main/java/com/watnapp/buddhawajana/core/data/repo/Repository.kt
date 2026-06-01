package com.watnapp.buddhawajana.core.data.repo

import kotlinx.coroutines.flow.Flow

interface Repository<T> {
    fun stream(): Flow<List<T>>
    suspend fun refresh(): Result<Unit>
}
