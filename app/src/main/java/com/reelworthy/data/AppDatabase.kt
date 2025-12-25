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
@Database(entities = [VideoEntity::class, PlaylistEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides the DAO for video-related operations.
     */
    abstract fun videoDao(): VideoDao

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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
