package com.zhiwei.math.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * API Key 加密存储（Android Keystore 主钥 + EncryptedSharedPreferences）。
 */
class ApiKeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "zhiwei_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API, key).apply()
    }

    fun apiKey(): String = prefs.getString(KEY_API, "").orEmpty()

    fun clear() {
        prefs.edit().remove(KEY_API).apply()
    }

    private companion object {
        const val KEY_API = "api_key"
    }
}
