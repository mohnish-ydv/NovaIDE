package com.mohnishraj.novaide.credentials

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small encrypted secret vault. Only ciphertext, IV and non-secret settings are persisted.
 * Secrets are never returned in lists or logs and each record is independently authenticated.
 */
class SecureCredentialStore(context: Context) {
    companion object {
        private const val KEY_ALIAS = "novaide_credentials_aes_v1"
        private const val MAX_SECRET_LENGTH = 16_384
    }

    private val prefs = context.getSharedPreferences("nova_credentials_secure", Context.MODE_PRIVATE)

    fun save(id: CredentialId, secret: String) {
        val normalized = secret.trim().trim('"', '\'')
        require(normalized.isNotEmpty()) { "Credential cannot be blank" }
        require(normalized.length <= MAX_SECRET_LENGTH) { "Credential is unexpectedly large" }
        require(normalized.none { it == '\u0000' }) { "Credential contains an invalid character" }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))
        val record = JSONObject().apply {
            put("v", 1)
            put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            put("savedAt", System.currentTimeMillis())
        }
        prefs.edit().putString(id.storageKey, record.toString()).apply()
    }

    fun read(id: CredentialId): String? {
        val raw = prefs.getString(id.storageKey, null) ?: return null
        return runCatching {
            val record = JSONObject(raw)
            val iv = Base64.decode(record.getString("iv"), Base64.NO_WRAP)
            val encrypted = Base64.decode(record.getString("data"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8).takeIf { it.isNotBlank() }
        }.getOrElse {
            delete(id)
            null
        }
    }

    fun has(id: CredentialId): Boolean = read(id) != null

    fun delete(id: CredentialId) {
        prefs.edit().remove(id.storageKey).apply()
    }

    fun configured(): Set<CredentialId> = CredentialId.entries.filterTo(linkedSetOf(), ::has)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
