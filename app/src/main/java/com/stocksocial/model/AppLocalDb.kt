package com.stocksocial.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Post::class], version = 1)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract fun postDao(): PostDao
}

object AppLocalDb {
    @Volatile
    private var instance: AppLocalDbRepository? = null

    fun getDatabase(context: Context): AppLocalDbRepository {
        return instance ?: synchronized(this) {
            val res = Room.databaseBuilder(
                context.applicationContext,
                AppLocalDbRepository::class.java,
                "db.db"
            ).fallbackToDestructiveMigration()
                .build()
            instance = res
            res
        }
    }
}
