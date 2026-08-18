package dev.tsdroid.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks")

class BookmarkStore(private val context: Context) {

    companion object {
        private const val TAG = "BookmarkStore"
        private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks_json")
        private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val KEY_LAST_BOOKMARK_ADDRESS = stringPreferencesKey("last_bookmark_address")
    }

    val bookmarks: Flow<List<ServerBookmark>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_BOOKMARKS] ?: "[]"
        parseBookmarks(json)
    }

    val autoReconnect: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_RECONNECT] ?: false
    }

    /**
     * One-time upgrade: encrypt bookmark passwords that were written by older
     * versions in plaintext. Runs at app startup and is safe to call again.
     */
    suspend fun migrateLegacyPasswords() {
        try {
            context.dataStore.edit { prefs ->
                val json = prefs[KEY_BOOKMARKS] ?: return@edit
                if (hasLegacyPassword(json)) {
                    prefs[KEY_BOOKMARKS] = serializeBookmarks(parseBookmarks(json))
                    Log.i(TAG, "Migrated bookmark passwords to Keystore encryption")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to migrate legacy bookmark passwords", e)
        }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_RECONNECT] = enabled
        }
    }

    suspend fun save(bookmarks: List<ServerBookmark>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BOOKMARKS] = serializeBookmarks(bookmarks)
        }
    }

    suspend fun add(bookmark: ServerBookmark) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]")
            prefs[KEY_BOOKMARKS] = serializeBookmarks(current + bookmark)
        }
    }

    suspend fun remove(index: Int) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]").toMutableList()
            if (index in current.indices) {
                current.removeAt(index)
                prefs[KEY_BOOKMARKS] = serializeBookmarks(current)
            }
        }
    }

    suspend fun replace(index: Int, bookmark: ServerBookmark) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]").toMutableList()
            if (index in current.indices) {
                // Preserve serverName and iconId from the old bookmark if not set
                val old = current[index]
                current[index] = bookmark.copy(
                    serverName = bookmark.serverName ?: old.serverName,
                    iconId = if (bookmark.iconId != 0L) bookmark.iconId else old.iconId,
                )
                prefs[KEY_BOOKMARKS] = serializeBookmarks(current)
            }
        }
    }

    /** Save the address of the last connected bookmark (for auto-reconnect). */
    suspend fun saveLastBookmarkAddress(address: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_BOOKMARK_ADDRESS] = address
        }
    }

    val lastBookmarkAddress: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BOOKMARK_ADDRESS] ?: ""
    }

    suspend fun updateServerInfo(address: String, info: dev.tslib.ServerInfo) {
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]")
            val now = System.currentTimeMillis()
            val updated = current.map { b ->
                if (b.address == address) {
                    b.copy(
                        serverName = info.name,
                        iconId = info.iconId,
                        platform = info.platform,
                        version = info.version,
                        maxClients = info.maxClients,
                        clientsOnline = info.clientsOnline,
                        channelsOnline = info.channelsOnline,
                        uptime = info.uptime,
                        lastSeenAt = now,
                    )
                } else {
                    b
                }
            }
            prefs[KEY_BOOKMARKS] = serializeBookmarks(updated)
        }
    }

    suspend fun addConnectedSeconds(address: String, seconds: Long) {
        if (seconds <= 0) return
        context.dataStore.edit { prefs ->
            val current = parseBookmarks(prefs[KEY_BOOKMARKS] ?: "[]")
            val updated = current.map { b ->
                if (b.address == address) b.copy(connectedSeconds = b.connectedSeconds + seconds) else b
            }
            prefs[KEY_BOOKMARKS] = serializeBookmarks(updated)
        }
    }

    private fun hasLegacyPassword(json: String): Boolean {
        if (json.isBlank() || json == "[]") return false
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).any { index ->
                val raw = array.optJSONObject(index)?.optString("password", "").orEmpty()
                raw.isNotEmpty() && raw != "null" && !SecretCipher.isEncrypted(raw)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun decryptPassword(raw: String): String? {
        val stored = raw.takeIf { it.isNotEmpty() && it != "null" } ?: return null
        // Encrypted values decrypt to the original password; legacy plaintext
        // passes through unchanged until the startup migration re-saves it.
        return SecretCipher.decrypt(stored)
    }

    /**
     * Keystore encryption should never crash bookmark saving on exotic devices;
     * if it is unavailable we keep the legacy plaintext value and retry the
     * migration on the next app start.
     */
    private fun encryptPassword(plain: String): String {
        return try {
            SecretCipher.encrypt(plain)
        } catch (e: Exception) {
            Log.e(TAG, "Keystore encryption unavailable; password stored unencrypted", e)
            plain
        }
    }

    private fun parseBookmarks(json: String): List<ServerBookmark> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val o = array.optJSONObject(index) ?: return@mapNotNull null
                ServerBookmark(
                    name = o.optString("name", ""),
                    address = o.optString("address", ""),
                    nickname = o.optString("nickname", ""),
                    password = decryptPassword(o.optString("password", "")),
                    channel = o.optString("channel", "").takeIf { it.isNotEmpty() && it != "null" },
                    serverName = o.optString("serverName", "").takeIf { it.isNotEmpty() && it != "null" },
                    iconId = o.optLong("iconId", 0),
                    iconEmoji = o.optString("iconEmoji", "").takeIf { it.isNotEmpty() && it != "null" },
                    platform = o.optString("platform", "").takeIf { it.isNotEmpty() && it != "null" },
                    version = o.optString("version", "").takeIf { it.isNotEmpty() && it != "null" },
                    maxClients = o.optInt("maxClients", 0),
                    clientsOnline = o.optInt("clientsOnline", 0),
                    channelsOnline = o.optInt("channelsOnline", 0),
                    uptime = o.optLong("uptime", 0),
                    lastSeenAt = o.optLong("lastSeenAt", 0),
                    connectedSeconds = o.optLong("connectedSeconds", 0),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeBookmarks(bookmarks: List<ServerBookmark>): String {
        val array = org.json.JSONArray()
        for (b in bookmarks) {
            val o = org.json.JSONObject()
            o.put("name", b.name)
            o.put("address", b.address)
            o.put("nickname", b.nickname)
            o.put("password", b.password?.let { encryptPassword(it) } ?: org.json.JSONObject.NULL)
            o.put("channel", b.channel ?: org.json.JSONObject.NULL)
            o.put("serverName", b.serverName ?: org.json.JSONObject.NULL)
            o.put("iconId", b.iconId)
            o.put("iconEmoji", b.iconEmoji ?: org.json.JSONObject.NULL)
            o.put("platform", b.platform ?: org.json.JSONObject.NULL)
            o.put("version", b.version ?: org.json.JSONObject.NULL)
            o.put("maxClients", b.maxClients)
            o.put("clientsOnline", b.clientsOnline)
            o.put("channelsOnline", b.channelsOnline)
            o.put("uptime", b.uptime)
            o.put("lastSeenAt", b.lastSeenAt)
            o.put("connectedSeconds", b.connectedSeconds)
            array.put(o)
        }
        return array.toString()
    }
}
