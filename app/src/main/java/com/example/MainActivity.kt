package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aura.ads.AuraAdManager
import com.example.aura.ui.AuraApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AuraAdManager.initialize(this)
        AuraAdManager.preloadInterstitial(this)
        setContent {
            AuraApp()
        }
    }
}

