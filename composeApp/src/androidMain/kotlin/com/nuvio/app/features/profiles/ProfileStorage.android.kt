package com.nuvio.app.features.profiles

import android.content.Context
import android.content.SharedPreferences

actual object ProfileStorage {
    private const val preferencesName = "nuvio_profile_cache"
    private const val payloadKey = "profile_payload"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(): String? =
        preferences?.getString(payloadKey, null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey, payload)
            ?.apply()
    }

    actual fun loadLastSelectionGreetingIndex(): Int? {
        val values = preferences ?: return null
        val key = "last_selection_greeting_index"
        return if (values.contains(key)) values.getInt(key, -1).takeIf { it >= 0 } else null
    }

    actual fun saveLastSelectionGreetingIndex(index: Int) {
        preferences
            ?.edit()
            ?.putInt("last_selection_greeting_index", index)
            ?.apply()
    }
}
