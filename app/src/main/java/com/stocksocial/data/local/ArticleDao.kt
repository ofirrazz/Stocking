package com.stocksocial.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.stocksocial.model.cache.CachedArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<CachedArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    suspend fun getAll(): List<CachedArticleEntity>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedArticleEntity?

    @Query("DELETE FROM articles")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CachedArticleEntity)

    @Transaction
    suspend fun replaceAll(items: List<CachedArticleEntity>) {
        deleteAll()
        insertAll(items)
    }
}
