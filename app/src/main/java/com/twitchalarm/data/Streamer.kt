package com.twitchalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streamers")
data class Streamer(
    @PrimaryKey
    val login: String,            // Twitch login (lowercase)
    val displayName: String = "", // Fetched from API
    val isLive: Boolean = false,  // Last known live status
    val streamTitle: String = "", // Title when went live
    val viewerCount: Int = 0,     // Viewer count
    val gameName: String = "",    // Category/game
    val notifyEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
