package com.nuvio.app.features.profiles

import platform.Foundation.NSUserDefaults

actual object ProfileStorage {
    private const val payloadKey = "profile_payload"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(payloadKey)

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = payloadKey)
    }

    actual fun loadLastSelectionGreetingIndex(): Int? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = "last_selection_greeting_index"
        return if (defaults.objectForKey(key) != null) {
            defaults.integerForKey(key).toInt().takeIf { it >= 0 }
        } else {
            null
        }
    }

    actual fun saveLastSelectionGreetingIndex(index: Int) {
        NSUserDefaults.standardUserDefaults.setInteger(index.toLong(), forKey = "last_selection_greeting_index")
    }
}
