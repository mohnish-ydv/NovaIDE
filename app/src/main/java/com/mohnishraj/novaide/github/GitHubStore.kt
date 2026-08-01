package com.mohnishraj.novaide.github

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.mohnishraj.novaide.credentials.CredentialId
import com.mohnishraj.novaide.credentials.SecureCredentialStore
import com.mohnishraj.novaide.git.GitHubRepository
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class GitHubStore(private val context: Context) {
    companion object {
        private const val LEGACY_KEY_ALIAS = "novaide_github_token_v1"
        private const val LEGACY_TOKEN_PREF = "github_token_encrypted"
    }

    private val prefs = context.getSharedPreferences("nova_github", Context.MODE_PRIVATE)
    private val secure = SecureCredentialStore(context)

    fun repository(workspaceUri: Uri): GitHubRepository? {
        val raw = prefs.getString(repoKey(workspaceUri), null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            GitHubRepository(json.getString("owner"), json.getString("name"), json.optString("branch", "main"))
        }.getOrNull()
    }

    fun saveRepository(workspaceUri: Uri, repository: GitHubRepository) {
        val json = JSONObject().apply {
            put("owner", repository.owner)
            put("name", repository.name)
            put("branch", repository.branch)
        }
        prefs.edit().putString(repoKey(workspaceUri), json.toString()).apply()
    }

    fun clearRepository(workspaceUri: Uri) {
        prefs.edit().remove(repoKey(workspaceUri)).apply()
    }

    fun saveToken(token: String) {
        val normalized = GitHubTokenNormalizer.normalize(token)
        secure.save(CredentialId.GITHUB, normalized)
        prefs.edit().remove(LEGACY_TOKEN_PREF).apply()
    }

    fun token(): String? {
        secure.read(CredentialId.GITHUB)?.let { value ->
            return runCatching { GitHubTokenNormalizer.normalize(value) }.getOrElse {
                secure.delete(CredentialId.GITHUB)
                null
            }
        }
        return migrateLegacyToken()
    }

    fun hasToken(): Boolean = token() != null

    fun normalizedToken(value: String): String = GitHubTokenNormalizer.normalize(value)

    fun clearToken() {
        secure.delete(CredentialId.GITHUB)
        prefs.edit().remove(LEGACY_TOKEN_PREF).apply()
    }

    private fun migrateLegacyToken(): String? {
        val raw = prefs.getString(LEGACY_TOKEN_PREF, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val iv = Base64.decode(json.getString("iv"), Base64.NO_WRAP)
            val data = Base64.decode(json.getString("data"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateLegacyKey(), GCMParameterSpec(128, iv))
            GitHubTokenNormalizer.normalize(String(cipher.doFinal(data), Charsets.UTF_8))
        }.onSuccess { migrated ->
            secure.save(CredentialId.GITHUB, migrated)
            prefs.edit().remove(LEGACY_TOKEN_PREF).apply()
        }.getOrElse {
            prefs.edit().remove(LEGACY_TOKEN_PREF).apply()
            null
        }
    }

    private fun repoKey(uri: Uri): String = "repo_${uri.toString().hashCode()}"

    private fun getOrCreateLegacyKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(LEGACY_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                LEGACY_KEY_ALIAS,
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
