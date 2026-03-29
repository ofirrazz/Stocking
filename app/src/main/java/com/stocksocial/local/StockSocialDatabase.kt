package com.stocksocial.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StockSocialDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao

    companion object {
        @Volatile
        private var INSTANCE: StockSocialDatabase? = null

        fun getInstance(context: Context): StockSocialDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockSocialDatabase::class.java,
                    "stocksocial.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
