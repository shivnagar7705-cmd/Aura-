package com.example.aura.veyora

import android.content.Context
import com.example.BuildConfig

/**
 * Configuration and metadata for Veyora Cloud backend integration.
 * Project: Test App 6
 * Project ID: c023f1e9-3baa-468a-9a92-4343182d94fb
 */
object VeyoraConfig {
    const val DEFAULT_PROJECT_ID = "c023f1e9-3baa-468a-9a92-4343182d94fb"
    const val PROJECT_NAME = "Test App 6"

    // Verified live demo project on Veyora Cloud
    const val DEMO_PROJECT_ID = "c74ba593-c43e-4c7a-b7af-b191a6038876"
    const val DEMO_API_KEY = "vk_a72cbfe5c912f4d273d0f2c4cf989c5e0fd4"

    private const val PREFS_NAME = "veyora_cloud_prefs"
    private const val KEY_PROJECT_ID = "project_id"
    private const val KEY_API_KEY = "api_key"

    @Volatile
    private var cachedApiKey: String? = null
    @Volatile
    private var cachedProjectId: String? = null

    /**
     * Resolves the Veyora Cloud base URL.
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
            "https://veyora-cloud.onrender.com"
        }

        return if (raw.endsWith("/")) raw else "$raw/"
    }

    /**
     * Saves user-configured credentials to persistent storage.
     */
    fun setCredentials(context: Context, projectId: String, apiKey: String) {
        val cleanProj = projectId.trim()
        val cleanKey = apiKey.trim()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_PROJECT_ID, cleanProj)
                .putString(KEY_API_KEY, cleanKey)
                .apply()
        } catch (_: Throwable) {}
        cachedProjectId = cleanProj
        cachedApiKey = cleanKey
    }

    /**
     * Retrieves the project API key.
     * Prefers locally configured SharedPreferences; otherwise falls back to BuildConfig.VEYORA_ANON_KEY.
     */
    fun getApiKey(context: Context? = null): String {
        cachedApiKey?.let { if (it.isNotBlank()) return it }
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val saved = prefs.getString(KEY_API_KEY, null)?.trim()
                if (!saved.isNullOrBlank()) {
                    cachedApiKey = saved
                    return saved
                }
            } catch (_: Throwable) {}
        }
        val key = try {
            val field = BuildConfig::class.java.getField("VEYORA_ANON_KEY")
            ((field.get(null) as? String) ?: "").trim()
        } catch (_: Throwable) {
            ""
        }
        return if (key == "public_anon_client_key" || key == "none" || key == "placeholder") "" else key
    }

    /**
     * Retrieves the optional public anonymous client key.
     * Preserved for backward compatibility.
     */
    fun getPublicAnonKey(): String = getApiKey(null)

    /**
     * Retrieves the active project ID.
     * Prefers locally configured SharedPreferences; otherwise falls back to BuildConfig.VEYORA_PROJECT_ID or DEFAULT_PROJECT_ID.
     */
    fun getProjectId(context: Context? = null): String {
        cachedProjectId?.let { if (it.isNotBlank()) return it }
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val saved = prefs.getString(KEY_PROJECT_ID, null)?.trim()
                if (!saved.isNullOrBlank()) {
                    cachedProjectId = saved
                    return saved
                }
            } catch (_: Throwable) {}
        }
        return try {
            val field = BuildConfig::class.java.getField("VEYORA_PROJECT_ID")
            val v = (field.get(null) as? String)?.trim()
            if (!v.isNullOrEmpty()) v else DEFAULT_PROJECT_ID
        } catch (_: Throwable) {
            DEFAULT_PROJECT_ID
        }
    }
}
