package dev.tsdroid.data

data class ServerBookmark(
    val name: String,
    val address: String,
    val nickname: String,
    val password: String? = null,
    val channel: String? = null,
    val serverName: String? = null,
    val iconId: Long = 0,
    // Cached server snapshot for the home tiles; refreshed after every
    // successful connection.
    val platform: String? = null,
    val version: String? = null,
    val maxClients: Int = 0,
    val clientsOnline: Int = 0,
    val channelsOnline: Int = 0,
    val uptime: Long = 0,
    val lastSeenAt: Long = 0,
)
