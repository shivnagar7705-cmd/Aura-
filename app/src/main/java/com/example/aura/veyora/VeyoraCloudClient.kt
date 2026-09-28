package com.example.aura.veyora

import android.util.Log
import com.example.aura.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class VeyoraCloudClient private constructor() {

    companion object {
        private const val TAG = "VeyoraCloudClient"

        @Volatile
        private var INSTANCE: VeyoraCloudClient? = null

        fun getInstance(): VeyoraCloudClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VeyoraCloudClient().also { INSTANCE = it }
            }
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Attempts to fetch songs from the Veyora Cloud / Supabase backend.
     * Tries standard PostgREST and Veyora Cloud endpoints in order.
     */
    suspend fun fetchSongs(): Result<Pair<List<Song>, String>> = withContext(Dispatchers.IO) {
        val baseUrl = VeyoraConfig.getBaseUrl()
        val anonKey = VeyoraConfig.getPublicAnonKey()
        val projectId = VeyoraConfig.getProjectId()

        // List of candidate endpoints to probe
        val candidateEndpoints = listOf(
            "${baseUrl}rest/v1/songs?select=*&order=created_at.desc",
            "${baseUrl}rest/v1/songs?select=*",
            "${baseUrl}rest/v1/tracks?select=*&order=created_at.desc",
            "${baseUrl}rest/v1/tracks?select=*",
            "${baseUrl}v1/projects/$projectId/songs",
            "${baseUrl}api/songs"
        ).distinct()

        var lastError: Exception? = null

        for (endpoint in candidateEndpoints) {
            try {
                val reqBuilder = Request.Builder()
                    .url(endpoint)
                    .header("Accept", "application/json")
                    .header("User-Agent", "AuraMusic/1.3 (Android)")

                if (anonKey.isNotBlank()) {
                    reqBuilder.header("apikey", anonKey)
                    reqBuilder.header("Authorization", "Bearer $anonKey")
                }

                val request = reqBuilder.build()
                httpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyString = response.body?.string().orEmpty()

                    if (code in 200..299) {
                        val parsed = VeyoraSongParser.parseSongList(bodyString)
                        if (parsed.isNotEmpty()) {
                            Log.d(TAG, "Successfully loaded ${parsed.size} songs from Veyora Cloud endpoint: $endpoint")
                            return@withContext Result.success(Pair(parsed, endpoint))
                        } else {
                            Log.d(TAG, "Endpoint $endpoint responded with code $code but 0 parsed songs.")
                        }
                    } else if (code == 404 || code == 400) {
                        Log.d(TAG, "Endpoint $endpoint returned code $code, attempting next candidate...")
                    } else {
                        Log.w(TAG, "Endpoint $endpoint returned HTTP $code: ${bodyString.take(200)}")
                        lastError = Exception("HTTP $code from Veyora Cloud ($endpoint)")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Connection attempt to $endpoint failed: ${e.message}")
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("No songs returned from Veyora Cloud endpoints"))
    }
}
