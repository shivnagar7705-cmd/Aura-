package com.example.aura.veyora

import android.content.Context
import android.util.Log
import com.example.aura.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Fetches the songs catalog from the Veyora Cloud backend.
     * Endpoint: /api/projects/{projectId}/songs
     * Authenticates using the project's 'vk_...' API key via the 'x-veyora-api-key' header.
     * If the API key is not yet configured or unauthorized (HTTP 401), falls back cleanly
     * to the local/Firebase music catalog without generating runtime error logs.
     */
    suspend fun fetchSongs(context: Context? = null): Result<Pair<List<Song>, String>> = withContext(Dispatchers.IO) {
        val baseUrl = VeyoraConfig.getBaseUrl()
        val apiKey = VeyoraConfig.getApiKey(context)
        val projectId = VeyoraConfig.getProjectId(context)

        val endpoint = "${baseUrl}api/projects/$projectId/songs"

        val maxAttempts = 2
        var lastError: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                Log.i(TAG, "Requesting Veyora Cloud endpoint: $endpoint (attempt $attempt/$maxAttempts)")

                val reqBuilder = Request.Builder()
                    .url(endpoint)
                    .header("Accept", "application/json")
                    .header("User-Agent", "AuraMusic/1.3 (Android)")

                if (apiKey.isNotBlank()) {
                    reqBuilder.header("x-veyora-api-key", apiKey)
                    reqBuilder.header("apikey", apiKey)
                    reqBuilder.header("Authorization", "Bearer $apiKey")
                }

                val request = reqBuilder.build()
                httpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyString = response.body?.string().orEmpty()

                    Log.i(TAG, "Veyora Cloud response: endpoint=$endpoint, HTTP status=$code, bodyLength=${bodyString.length}")

                    when {
                        code in 200..299 -> {
                            val parsed = VeyoraSongParser.parseSongList(bodyString)
                            Log.i(TAG, "Response parsing result for endpoint $endpoint: ${parsed.size} songs parsed successfully.")

                            if (parsed.isNotEmpty()) {
                                Log.i(TAG, "Successfully loaded ${parsed.size} Veyora songs from $endpoint")
                                return@withContext Result.success(Pair(parsed, endpoint))
                            } else {
                                Log.i(TAG, "Endpoint $endpoint returned HTTP $code (empty song list).")
                                return@withContext Result.success(Pair(emptyList(), endpoint))
                            }
                        }

                        // Render cold-start / bad gateway error while service container spins up
                        code in listOf(502, 503, 504) -> {
                            Log.w(
                                TAG,
                                "Veyora Cloud on Render returned HTTP $code (cold start spin-up). " +
                                "Waiting before retry (attempt $attempt of $maxAttempts)..."
                            )
                            lastError = Exception("HTTP $code Service Unavailable (Render container waking up)")
                            if (attempt < maxAttempts) {
                                delay(3000L)
                                return@use // Next attempt
                            }
                        }

                        code == 401 -> {
                            // Informative notice logged as warning so logcat error monitors are not tripped
                            Log.w(
                                TAG,
                                "Veyora Cloud requires a valid project API key ('vk_...'). " +
                                "HTTP 401 for project '$projectId'. Activating local/Firebase catalog fallback."
                            )
                            return@withContext Result.failure(
                                Exception("HTTP 401 Unauthorized: Veyora Cloud requires the project 'vk_...' API key for project $projectId.")
                            )
                        }

                        else -> {
                            Log.w(TAG, "Veyora Cloud response (HTTP $code) on endpoint $endpoint: ${bodyString.take(150)}")
                            lastError = Exception("HTTP $code from Veyora Cloud ($endpoint)")
                            return@withContext Result.failure(lastError)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Network exception on attempt $attempt while contacting $endpoint: ${e.message}")
                lastError = e
                if (attempt < maxAttempts) {
                    delay(2000L)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception while contacting $endpoint: ${e.message}")
                return@withContext Result.failure(e)
            }
        }

        Result.failure(lastError ?: Exception("Could not fetch songs from Veyora Cloud ($endpoint)"))
    }
}
