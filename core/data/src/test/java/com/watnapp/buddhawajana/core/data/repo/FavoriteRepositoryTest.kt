package com.watnapp.buddhawajana.core.data.repo

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.db.FavoriteDao
import com.watnapp.buddhawajana.core.data.db.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteRepositoryTest {
    private val backing = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    private val dao = object : FavoriteDao {
        override fun stream(): Flow<List<FavoriteEntity>> =
            backing.map { it.sortedByDescending { e -> e.addedAt } }
        override fun isFavorite(audioId: String): Flow<Boolean> =
            backing.map { list -> list.any { it.audioId == audioId } }
        override suspend fun exists(audioId: String): Boolean = backing.value.any { it.audioId == audioId }
        override suspend fun insert(e: FavoriteEntity) =
            backing.update { it.filterNot { x -> x.audioId == e.audioId } + e }
        override suspend fun deleteById(audioId: String) =
            backing.update { it.filterNot { x -> x.audioId == audioId } }
    }
    private fun fav(id: String, addedAt: Long) =
        com.watnapp.buddhawajana.core.model.Favorite(id, "T$id", "u$id", "9", "Album", null, addedAt)

    @Test fun `add then favorites contains it newest-first`() = runTest {
        val repo = FavoriteRepository(dao)
        repo.add(fav("1", 100))
        repo.add(fav("2", 200))
        repo.favorites.test {
            assertEquals(listOf("2", "1"), awaitItem().map { it.audioId })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `toggle adds when absent and removes when present`() = runTest {
        val repo = FavoriteRepository(dao)
        repo.toggle(fav("1", 1))
        repo.isFavorite("1").test { assertTrue(awaitItem()); cancelAndIgnoreRemainingEvents() }
        repo.toggle(fav("1", 2))
        repo.isFavorite("1").test { assertFalse(awaitItem()); cancelAndIgnoreRemainingEvents() }
    }

    @Test fun `remove deletes`() = runTest {
        val repo = FavoriteRepository(dao)
        repo.add(fav("1", 1))
        repo.remove("1")
        repo.isFavorite("1").test { assertFalse(awaitItem()); cancelAndIgnoreRemainingEvents() }
    }
}
