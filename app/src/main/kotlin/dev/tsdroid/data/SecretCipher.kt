package dev.tsdroid.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small AES-GCM wrapper backed by the Android Keystore.
 *
 * The key itself never leaves the secure hardware (or the OS keymaster
 * process), so even a rooted device that extracts app data cannot recover
 * stored server passwords or the TS identity private key without also
 * extracting the keystore key — which Android protects independently.
 *
 * Values are stored as `enc:v1:<base64(iv|ciphertext)>`. Legacy plaintext
 * values decrypt to themselves so old bookmarks keep working until migration.
 */
object SecretCipher {
    private const val TAG = "SecretCipher"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ts6droid_master_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "enc:v1:"
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_BITS = 128

    @Volatile
    private var cachedKey: SecretKey? = null
    private val keyLock = Any()

    fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtainKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encrypted, 0, payload, iv.size, encrypted.size)
        return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    /**
     * Decrypts a stored value. Plain legacy values are returned unchanged;
     * encrypted values that cannot be decrypted (corrupted data or keystore
     * invalidation) return `null` so callers can fail open instead of crash.
     */
    fun decrypt(value: String): String? {
        if (!isEncrypted(value)) return value
        return try {
            val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
            require(payload.size > IV_LENGTH_BYTES) { "Encrypted payload is too short" }
            val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
            val encrypted = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                obtainKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv),
            )
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to decrypt secret; treating it as unavailable", e)
            null
        }
    }

    private fun obtainKey(): SecretKey {
        cachedKey?.let { return it }
        return synchronized(keyLock) {
            cachedKey ?: loadOrCreateKey().also { cachedKey = it }
        }
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
