package com.reelworthy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val itemCount: Int,
    val lastSyncTime: Long = 0
)
