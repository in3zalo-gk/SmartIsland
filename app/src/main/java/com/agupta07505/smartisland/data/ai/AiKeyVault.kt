/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps provider API keys encrypted with a device-bound Android Keystore key. */
class AiKeyVault(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "smart_island_ai_secrets",
        Context.MODE_PRIVATE
    )

    fun save(providerId: String, apiKey: String) {
        val normalized = apiKey.trim()
        require(normalized.isNotEmpty()) { "API key cannot be empty" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))
        val safeId = providerId.filter { it.isLetterOrDigit() || it == '_' }.take(32)
        preferences.edit()
            .putString("${safeId}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("${safeId}_ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    fun load(providerId: String): String? {
        val safeId = providerId.filter { it.isLetterOrDigit() || it == '_' }.take(32)
        val iv = preferences.getString("${safeId}_iv", null) ?: return null
        val ciphertext = preferences.getString("${safeId}_ciphertext", null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun hasKey(providerId: String): Boolean = !load(providerId).isNullOrBlank()

    fun clear(providerId: String) {
        val safeId = providerId.filter { it.isLetterOrDigit() || it == '_' }.take(32)
        preferences.edit()
            .remove("${safeId}_iv")
            .remove("${safeId}_ciphertext")
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "smart_island_ai_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
