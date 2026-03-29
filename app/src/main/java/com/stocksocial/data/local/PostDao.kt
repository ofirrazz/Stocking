package com.stocksocial.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stocksocial.model.cache.CachedPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<CachedPostEntity>>

    @Query("SELECT * FROM posts ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<CachedPostEntity>

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedPostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CachedPostEntity)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}
