package org.bookie.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Only AES-GCM ciphertext is saved in SharedPreferences.
 * The AES key is non-exportable and managed by Android Keystore (API 24+).
 */
class AndroidSecureSessionStore(context: Context) : SecureSessionStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(): StoredAuthSession? = withContext(Dispatchers.IO) {
        val saved = prefs.getString(PAYLOAD, null) ?: return@withContext null
        val pieces = saved.split(':', limit = 2)
        require(pieces.size == 2) { "El almacenamiento de sesión está dañado." }
        val iv = Base64.decode(pieces[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(pieces[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        json.decodeFromString<StoredAuthSession>(cipher.doFinal(encrypted).decodeToString())
    }

    override suspend fun write(session: StoredAuthSession) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(json.encodeToString(StoredAuthSession.serializer(), session).encodeToByteArray())
        val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(prefs.edit().putString(PAYLOAD, value).commit()) {
            "No se pudo guardar la sesión cifrada."
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        check(prefs.edit().remove(PAYLOAD).commit()) {
            "No se pudo borrar la sesión cifrada."
        }
    }

    private fun key(): SecretKey {
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keystore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "bookie_secure_auth"
        const val PAYLOAD = "session_ciphertext"
        const val KEY_ALIAS = "bookie_auth_aes_v1"
        const val CIPHER = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}

actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1_000L
