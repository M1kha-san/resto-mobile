package com.example.restoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.restoapp.navigation.AppNavigation
import com.example.restoapp.ui.theme.RestoAppTheme

/**
 * Activity utama aplikasi RestoApp.
 *
 * Mengatur tema dan navigasi utama aplikasi.
 * Edge-to-edge diaktifkan agar tampilan lebih modern
 * dengan status bar transparan.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RestoAppTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}

