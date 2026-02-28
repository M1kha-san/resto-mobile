package com.example.restoapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.restoapp.screens.user.DineInEntryScreen
import com.example.restoapp.screens.user.MenuScreenWrapper
import com.example.restoapp.screens.user.TakeAwayEntryScreen
import com.example.restoapp.screens.welcome.WelcomeScreen

/**
 * Navigasi utama aplikasi RestoApp.
 *
 * Alur navigasi:
 * 1. Welcome Screen → Pilih metode pemesanan (Dine In / Take Away)
 * 2. DineIn Entry → Input nama + pilih meja
 *    TakeAway Entry → Input nama + nomor telepon
 * 3. Menu Screen → Lihat menu, detail, dan keranjang belanja
 *
 * Catatan: Route login dan dashboard pegawai di-comment karena
 * fitur tersebut belum diimplementasikan di versi mobile ini.
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController, startDestination = "welcome") {

        // Halaman selamat datang - pilih metode pemesanan
        composable("welcome") {
            WelcomeScreen(navController = navController)
        }

        // Halaman entry Dine In - input nama dan pilih meja
        composable("user_entry_dinein") {
            DineInEntryScreen(navController = navController)
        }

        // Halaman entry Take Away - input nama dan nomor telepon
        composable("user_entry_takeaway") {
            TakeAwayEntryScreen(navController = navController)
        }

        // Halaman menu - menampilkan daftar menu, detail, dan daftar pesanan
        // Parameter: customerName (nama pelanggan), mejaId (0 = take away)
        composable(
            route = "menu/{customerName}/{mejaId}",
            arguments = listOf(
                navArgument("customerName") { type = NavType.StringType },
                navArgument("mejaId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val customerName = backStackEntry.arguments?.getString("customerName") ?: ""
            val mejaId = backStackEntry.arguments?.getInt("mejaId") ?: 0

            MenuScreenWrapper(
                parentNavController = navController,
                customerName = customerName,
                mejaId = mejaId
            )
        }

        // Route untuk login pegawai (belum diimplementasikan)
        // composable("login") { LoginScreen(navController) }

        // Route untuk dashboard pegawai (belum diimplementasikan)
        // composable("pegawai_dashboard") { DashboardScreen(navController) }
    }
}

