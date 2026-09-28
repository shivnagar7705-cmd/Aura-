package com.example.aura.veyora

import com.example.BuildConfig

/**
 * Configuration and metadata for Veyora Cloud backend integration.
 * Project: Test App 6
 * Project ID: c023f1e9-3baa-468a-9a92-4343182d94fb
 */
object VeyoraConfig {
    const val DEFAULT_PROJECT_ID = "c023f1e9-3baa-468a-9a92-4343182d94fb"
    const val PROJECT_NAME = "Test App 6"

    /**
     * Resolves the Veyora Cloud base URL.
     * Prefers BuildConfig.VEYORA_CLOUD_URL if set; otherwise defaults to the Supabase PostgREST endpoint.
     */
    fun getBaseUrl(): String {
        val configured = try {
            val field = BuildConfig::class.java.getField("VEYORA_CLOUD_URL")
            field.get(null) as? String
        } catch (_: Throwable) {
            null
        }

        val raw = if (!configured.isNullOrBlank()) {
            configured.trim()
        } else {
            "https://$DEFAULT_PROJECT_ID.supabase.co"
        }

        return if (raw.endsWith("/")) raw else "$raw/"
    }

    /**
     * Retrieves the optional public anonymous client key.
     * Note: Secret/service-role keys are strictly forbidden on client devices.
     */
    fun getPublicAnonKey(): String {
        val key = try {
            val field = BuildConfig::class.java.getField("VEYORA_ANON_KEY")
            ((field.get(null) as? String) ?: "").trim()
        } catch (_: Throwable) {
            ""
        }
        return if (key == "public_anon_client_key" || key == "none" || key == "placeholder") "" else key
    }

    /**
     * Retrieves the active project ID.
     */
    fun getProjectId(): String {
        return try {
            val field = BuildConfig::class.java.getField("VEYORA_PROJECT_ID")
            val v = (field.get(null) as? String)?.trim()
            if (!v.isNullOrEmpty()) v else DEFAULT_PROJECT_ID
        } catch (_: Throwable) {
            DEFAULT_PROJECT_ID
        }
    }
}
