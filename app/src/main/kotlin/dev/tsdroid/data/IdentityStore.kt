package dev.tsdroid.data

import android.content.Context
import android.util.Log
import dev.tsdroid.diag.DiagLog
import dev.tslib.Identity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads and creates the TeamSpeak identity while keeping its private key
 * encrypted at rest with the Android Keystore (via [SecretCipher]).
 *
 * Migration path:
 *  - `identity.enc` exists  -> decrypt, parse, use.
 *  - only `identity.ini`    -> legacy plaintext from older builds; parse it,
 *    write the encrypted copy, then delete the plaintext file.
 *  - neither                -> generate a fresh identity and store it encrypted.
 *
 * If decryption ever fails (keystore invalidation after a broken backup
 * restore, data corruption), the corrupt file is renamed for diagnostics and
 * a fresh identity is generated — the app must never crash at startup.
 */
class IdentityStore(private val context: Context) {

    companion object {
        private const val TAG = "IdentityStore"
    }

    private val legacyFile = File(context.filesDir, "identity.ini")
    private val encryptedFile = File(context.filesDir, "identity.enc")

    suspend fun getOrCreateIdentity(): Identity = withContext(Dispatchers.IO) {
        if (encryptedFile.exists()) {
            loadDecryptedIdentity()?.let { return@withContext it }
            backUpCorruptEncryptedFile()
            return@withContext createAndStore()
        }

        if (legacyFile.exists()) {
            migrateLegacyIdentity()?.let { return@withContext it }
            backUpLegacyFile()
        }

        createAndStore()
    }

    private fun loadDecryptedIdentity(): Identity? {
        return try {
            val encrypted = encryptedFile.readText().trim()
            val plain = SecretCipher.decrypt(encrypted)
            if (plain == null) {
                DiagLog.w(TAG, "Could not decrypt identity.enc")
                null
            } else {
                Identity.fromString(plain)
            }
        } catch (t: Throwable) {
            DiagLog.w(TAG, "Failed to load encrypted identity", t)
            null
        }
    }

    private fun migrateLegacyIdentity(): Identity? {
        return try {
            val plain = legacyFile.readText().trim()
            val identity = Identity.fromString(plain)
            writeEncrypted(SecretCipher.encrypt(plain))
            if (!legacyFile.delete()) {
                Log.w(TAG, "Legacy identity could not be deleted; retrying next start")
            } else {
                DiagLog.i(TAG, "Migrated legacy identity.ini to Keystore-encrypted storage")
            }
            identity
        } catch (t: Throwable) {
            DiagLog.w(TAG, "Failed to migrate legacy identity", t)
            null
        }
    }

    private fun createAndStore(): Identity {
        val identity = Identity()
        try {
            writeEncrypted(SecretCipher.encrypt(identity.exportString()))
            DiagLog.i(TAG, "Created new Keystore-encrypted identity")
        } catch (t: Throwable) {
            // Identity itself is still usable in memory for this session.
            DiagLog.e(TAG, "Failed to persist new identity", t)
        }
        return identity
    }

    private fun writeEncrypted(content: String) {
        val tmp = File.createTempFile("identity", ".tmp", context.filesDir)
        try {
            tmp.writeText(content)
            if (!tmp.renameTo(encryptedFile)) {
                encryptedFile.writeText(content)
                tmp.delete()
            }
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    private fun backUpCorruptEncryptedFile() {
        val backup = File(context.filesDir, "identity.enc.corrupt-${System.currentTimeMillis()}")
        try {
            encryptedFile.renameTo(backup)
        } catch (_: Throwable) {
        }
    }

    private fun backUpLegacyFile() {
        val backup = File(context.filesDir, "identity.ini.broken-${System.currentTimeMillis()}")
        try {
            legacyFile.renameTo(backup)
        } catch (_: Throwable) {
        }
    }
}
