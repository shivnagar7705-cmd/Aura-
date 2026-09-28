package com.example.aura.veyora

sealed class VeyoraConnectionStatus {
    object Idle : VeyoraConnectionStatus()
    object Connecting : VeyoraConnectionStatus()
    data class Connected(
        val songCount: Int,
        val projectName: String,
        val endpointUsed: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : VeyoraConnectionStatus()
    data class Fallback(
        val reason: String,
        val fallbackCatalogSize: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : VeyoraConnectionStatus()
}
