package com.example.restoapp.screens.user

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.restoapp.api.CustomerApiService
import com.example.restoapp.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wrapper navigasi untuk alur pemesanan menu.
 * Mengelola navigasi antara daftar menu, detail menu, dan keranjang.
 */
@Composable
fun MenuScreenWrapper(
    parentNavController: NavController,
    customerName: String,
    mejaId: Int
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State keranjang belanja (dipakai bersama oleh semua halaman)
    var cart by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var selectedMenu by remember { mutableStateOf<Menu?>(null) }
    var isSubmittingOrder by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = "menu") {

        // ===== Halaman Daftar Menu =====
        composable("menu") {
            MenuScreenNew(
                navController = navController,
                customerName = customerName,
                cart = cart,
                onMenuClick = { menu ->
                    selectedMenu = menu
                    navController.navigate("menu_detail")
                },
                onCartClick = {
                    if (cart.isNotEmpty()) {
                        navController.navigate("order_list")
                    } else {
                        Toast.makeText(context, "Keranjang kosong", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // ===== Halaman Detail Menu =====
        composable("menu_detail") {
            selectedMenu?.let { menu ->
                MenuDetailScreen(
                    navController = navController,
                    menu = menu,
                    onAddToCart = { selectedMenuParam, quantity, note ->
                        cart = addOrUpdateCartItem(cart, selectedMenuParam, quantity, note, mejaId)
                    }
                )
            } ?: run {
                // Kembali jika menu tidak ditemukan
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        // ===== Halaman Daftar Pesanan / Keranjang =====
        composable("order_list") {
            OrderListScreen(
                navController = navController,
                customerName = customerName,
                cart = cart,
                onUpdateQuantity = { item, newQuantity ->
                    cart = updateCartItemQuantity(cart, item, newQuantity)
                },
                onUpdateNote = { item, newNote ->
                    cart = updateCartItemNote(cart, item, newNote)
                },
                onCheckout = {
                    if (isSubmittingOrder) return@OrderListScreen
                    scope.launch {
                        isSubmittingOrder = true
                        submitOrder(
                            customerName = customerName,
                            mejaId = mejaId,
                            cart = cart,
                            onSuccess = { transaksi ->
                                isSubmittingOrder = false
                                Toast.makeText(
                                    context,
                                    "Pesanan berhasil!\nTotal: ${formatToRupiah(transaksi.totalBayar)}",
                                    Toast.LENGTH_LONG
                                ).show()
                                cart = emptyList()
                                parentNavController.navigate("welcome") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onFailure = { errorMsg ->
                                isSubmittingOrder = false
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                onClearCart = {
                    cart = emptyList()
                    Toast.makeText(context, "Keranjang dikosongkan", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ===== HELPER KERANJANG =====

private fun addOrUpdateCartItem(
    cart: List<CartItem>,
    menu: Menu,
    quantity: Int,
    note: String,
    mejaId: Int
): List<CartItem> {
    val existingItem = cart.find { it.menu.menuId == menu.menuId }
    return if (existingItem != null) {
        // Update jumlah dan catatan item yang sudah ada
        cart.map {
            if (it.menu.menuId == menu.menuId) {
                it.copy(
                    quantity = it.quantity + quantity,
                    catatan = note.ifEmpty { it.catatan }
                )
            } else it
        }
    } else {
        // Tambah item baru ke keranjang
        cart + CartItem(
            menu = menu,
            quantity = quantity,
            catatan = note,
            metodePesanan = if (mejaId == 0) MetodePesanan.TAKE_AWAY else MetodePesanan.DINE_IN
        )
    }
}

private fun updateCartItemQuantity(cart: List<CartItem>, item: CartItem, newQuantity: Int): List<CartItem> {
    return if (newQuantity > 0) {
        cart.map {
            if (it.menu.menuId == item.menu.menuId) it.copy(quantity = newQuantity)
            else it
        }
    } else {
        cart.filter { it.menu.menuId != item.menu.menuId }
    }
}

private fun updateCartItemNote(cart: List<CartItem>, item: CartItem, newNote: String): List<CartItem> {
    return cart.map {
        if (it.menu.menuId == item.menu.menuId) it.copy(catatan = newNote)
        else it
    }
}

private suspend fun submitOrder(
    customerName: String,
    mejaId: Int,
    cart: List<CartItem>,
    onSuccess: (Transaksi) -> Unit,
    onFailure: (String) -> Unit
) {
    val result = withContext(Dispatchers.IO) {
        CustomerApiService.createTransaksiFromCart(
            customerName = customerName,
            mejaId = mejaId,
            cartItems = cart
        )
    }
    withContext(Dispatchers.Main) {
        result.fold(
            onSuccess = { onSuccess(it) },
            onFailure = { onFailure(it.message ?: "Terjadi kesalahan") }
        )
    }
}

