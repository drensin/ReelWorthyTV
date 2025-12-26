package com.reelworthy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main Room Database for the application.
 *
 * Defines the database configuration and serves as the main access point to the persisted data.
 */
@Database(entities = [VideoEntity::class, PlaylistEntity::class, SearchHistoryEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides the DAO for video-related operations.
     */
    abstract fun videoDao(): VideoDao

    /**
     * Provides the DAO for search history operations.
     */
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of the database.
         *
         * @param context The application context.
         * @return The singleton [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reelworthy_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
