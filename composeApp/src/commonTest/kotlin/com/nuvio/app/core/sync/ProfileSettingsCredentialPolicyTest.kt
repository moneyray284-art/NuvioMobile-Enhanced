package com.nuvio.app.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileSettingsCredentialPolicyTest {
    @Test
    fun `credential fields are removed from profile settings payloads`() {
        val payload = buildJsonObject {
            put("debrid_enabled", JsonPrimitive(true))
            put("debrid_torbox_api_key", JsonPrimitive("secret"))
            put("debrid_premiumize_api_key", JsonPrimitive("secret"))
        }

        val sanitized = withoutProfileCredentials(PROFILE_DEBRID_SETTINGS_FEATURE, payload)

        assertFalse("debrid_torbox_api_key" in sanitized)
        assertFalse("debrid_premiumize_api_key" in sanitized)
        assertEquals(JsonPrimitive(true), sanitized["debrid_enabled"])
    }

    @Test
    fun `legacy remote credential fields cannot overwrite local credentials`() {
        val remote = buildJsonObject {
            put("mdblist_enabled", JsonPrimitive(true))
            put("mdblist_api_key", JsonPrimitive("remote"))
        }
        val local = buildJsonObject {
            put("mdblist_api_key", JsonPrimitive("local"))
        }

        val merged = preservingLocalProfileCredentials(PROFILE_MDBLIST_SETTINGS_FEATURE, remote, local)

        assertEquals(JsonPrimitive("local"), merged["mdblist_api_key"])
        assertEquals(JsonPrimitive(true), merged["mdblist_enabled"])
    }

    @Test
    fun `local TorBox and Premiumize keys win over blank or not set remote values`() {
        val remote = buildJsonObject {
            put("debrid_torbox_api_key", encodeSyncString(" "))
            put("debrid_premiumize_api_key", encodeSyncString("Not set"))
        }
        val local = buildJsonObject {
            put("debrid_torbox_api_key", encodeSyncString("local-torbox-key"))
            put("debrid_premiumize_api_key", encodeSyncString("local-premiumize-key"))
        }

        val merged = preservingLocalProfileCredentials(PROFILE_DEBRID_SETTINGS_FEATURE, remote, local)

        assertEquals("local-torbox-key", merged.decodeSyncString("debrid_torbox_api_key"))
        assertEquals("local-premiumize-key", merged.decodeSyncString("debrid_premiumize_api_key"))
    }

    @Test
    fun `blank or placeholder remote debrid keys are discarded when local keys are absent`() {
        val remote = buildJsonObject {
            put("debrid_torbox_api_key", encodeSyncString(""))
            put("debrid_premiumize_api_key", encodeSyncString("Not set"))
        }

        val merged = preservingLocalProfileCredentials(
            PROFILE_DEBRID_SETTINGS_FEATURE,
            remote,
            buildJsonObject { },
        )

        assertFalse("debrid_torbox_api_key" in merged)
        assertFalse("debrid_premiumize_api_key" in merged)
    }

    @Test
    fun `legacy TMDB credentials are discarded from local and remote settings`() {
        val remote = buildJsonObject {
            put("tmdb_enabled", JsonPrimitive(true))
            put("tmdb_api_key", JsonPrimitive("remote"))
        }
        val local = buildJsonObject {
            put("tmdb_api_key", JsonPrimitive("local"))
        }

        val merged = preservingLocalProfileCredentials(PROFILE_TMDB_SETTINGS_FEATURE, remote, local)
        val sanitized = withoutProfileCredentials(PROFILE_TMDB_SETTINGS_FEATURE, local)

        assertFalse("tmdb_api_key" in merged)
        assertFalse("tmdb_api_key" in sanitized)
        assertEquals(JsonPrimitive(true), merged["tmdb_enabled"])
    }
}
