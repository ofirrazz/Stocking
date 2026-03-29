package com.stocksocial.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stocksocial.model.cache.CachedArticleEntity
import com.stocksocial.model.cache.CachedPostEntity

@Database(
    entities = [CachedArticleEntity::class, CachedPostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun postDao(): PostDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stocksocial.db"
                ).build().also { instance = it }
            }
    }
}
