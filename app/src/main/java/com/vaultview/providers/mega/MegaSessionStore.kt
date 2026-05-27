package com.vaultview.providers.mega

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface MegaSessionStore {
    fun loadSessionId(): String?
    fun saveSessionId(sessionId: String)
    fun clear()
}

class SharedPreferencesMegaSessionStore(context: Context) : MegaSessionStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "mega_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun loadSessionId(): String? = preferences.getString(KEY_SESSION_ID, null)

    override fun saveSessionId(sessionId: String) {
        preferences.edit().putString(KEY_SESSION_ID, sessionId).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_SESSION_ID = "session_id"
    }
}
