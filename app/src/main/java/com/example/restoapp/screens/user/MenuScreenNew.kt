package com.example.restoapp.screens.user

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.restoapp.api.CustomerApiService
import com.example.restoapp.models.CartItem
import com.example.restoapp.models.Menu
import com.example.restoapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreenNew(
    navController: NavController,
    customerName: String,
    cart: List<CartItem>,
    onMenuClick: (Menu) -> Unit,
    onCartClick: () -> Unit
) {
    var menuList by remember { mutableStateOf<List<Menu>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showContent by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoadingMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val categories = remember(menuList) {
        mutableListOf("Semua").apply {
            addAll(menuList.map { it.kategori }.distinct().sorted())
        }
    }

    // Filter menu berdasarkan kategori dan kata kunci pencarian
    val filteredMenu = remember(menuList, selectedCategory, searchQuery) {
        menuList.filter { menu ->
            val matchCategory = selectedCategory == "Semua" || menu.kategori.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isEmpty() || menu.namaMenu.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    // Kelompokkan menu per 2 item untuk tampilan grid
    val menuPairs = remember(filteredMenu) { filteredMenu.chunked(2) }

    // Total harga keranjang
    val cartTotal = remember(cart) { cart.sumOf { it.menu.harga * it.quantity } }

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
        isLoadingMenu = true
        val result = withContext(Dispatchers.IO) { CustomerApiService.getMenu() }
        result.fold(
            onSuccess = { menuList = it },
            onFailure = { Toast.makeText(context, "Gagal memuat menu: ${it.message}", Toast.LENGTH_SHORT).show() }
        )
        isLoadingMenu = false
    }

    Box(modifier = Modifier.fillMaxSize().background(CreamBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header: logo, info pelanggan, dan kolom pencarian
            item { MenuHeader(customerName, cart, searchQuery, { searchQuery = it }, onCartClick) }

            // Chip filter kategori (scroll horizontal)
            item { CategoryChips(categories, selectedCategory) { selectedCategory = it } }

            // Judul bagian menu
            item { MenuSectionHeader(searchQuery, selectedCategory, isLoadingMenu, filteredMenu.size) }

            // Konten utama: loading / kosong / grid menu
            when {
                isLoadingMenu -> item { LoadingState() }
                filteredMenu.isEmpty() -> item { EmptyState() }
                else -> {
                    items(menuPairs) { pair ->
                        MenuGridRow(pair, onMenuClick)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Bar keranjang di bagian bawah (muncul jika ada item di keranjang)
        FloatingCartBar(
            visible = showContent && cart.isNotEmpty(),
            cartSize = cart.size,
            cartTotal = cartTotal,
            onCartClick = onCartClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MenuHeader(
    customerName: String,
    cart: List<CartItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCartClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = OrangePrimary) {
        Column(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
        ) {
            // Baris atas: logo dan ikon keranjang
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Restaurant, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    Text("RestoApp", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                CartIconWithBadge(cartSize = cart.size, onClick = { if (cart.isNotEmpty()) onCartClick() })
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Kartu info pelanggan
            CustomerInfoCard(customerName, cart.size)

            Spacer(modifier = Modifier.height(16.dp))

            // Kolom pencarian
            SearchField(searchQuery, onSearchChange)
        }
    }
}

@Composable
private fun CartIconWithBadge(cartSize: Int, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        if (cartSize > 0) {
            Box(
                modifier = Modifier.size(18.dp).clip(CircleShape)
                    .background(Color(0xFFFF3B30)).align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$cartSize", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun CustomerInfoCard(customerName: String, cartSize: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Nama Pelanggan", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                Text(customerName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            HorizontalDivider(modifier = Modifier.width(1.dp).height(36.dp), color = Color.White.copy(alpha = 0.3f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Total Item", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                Text("$cartSize item", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun SearchField(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50.dp), color = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
            SimpleTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Temukan Makanan Favorit",
                modifier = Modifier.weight(1f)
            )
            if (searchQuery.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close, null,
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(18.dp).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSearchChange("") }
                )
            }
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(20.dp))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            Surface(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSelect(category) },
                shape = RoundedCornerShape(50.dp),
                color = if (isSelected) OrangePrimary else Color.White,
                border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE0D8D0)) else null
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color(0xFF666666)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun MenuSectionHeader(
    searchQuery: String,
    selectedCategory: String,
    isLoading: Boolean,
    menuCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(OrangePrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.RestaurantMenu, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(
            text = when {
                searchQuery.isNotEmpty() -> "Hasil Pencarian"
                selectedCategory == "Semua" -> "Menu Populer"
                else -> selectedCategory
            },
            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E)
        )
        if (!isLoading && menuCount > 0) {
            Text("($menuCount)", fontSize = 13.sp, color = Color(0xFFAAAAAA))
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = OrangePrimary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            Text("Memuat menu...", fontSize = 14.sp, color = Color(0xFFAAAAAA))
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🍽️", fontSize = 56.sp)
            Text("Menu tidak ditemukan", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
            Text("Coba kata kunci lain", fontSize = 13.sp, color = Color(0xFFAAAAAA))
        }
    }
}

@Composable
private fun MenuGridRow(pair: List<Menu>, onMenuClick: (Menu) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MenuGridCard(menu = pair[0], modifier = Modifier.weight(1f), onClick = { onMenuClick(pair[0]) })
        if (pair.size > 1) {
            MenuGridCard(menu = pair[1], modifier = Modifier.weight(1f), onClick = { onMenuClick(pair[1]) })
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MenuGridCard(menu: Menu, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF0EBE3))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Area gambar / emoji
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp)
                    .background(if (menu.ketersediaan) OrangeSoft else Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(getEmojiForCategory(menu.kategori), fontSize = 64.sp, textAlign = TextAlign.Center)

                // Overlay jika menu habis
                if (!menu.ketersediaan) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFCC0000)) {
                            Text("Habis", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            // Area keterangan
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    menu.namaMenu, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E), maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Filled.Star, null, tint = GoldStar, modifier = Modifier.size(13.dp))
                    Text("4.8", fontSize = 12.sp, color = Color(0xFF666666))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    formatToRupiah(menu.harga),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OrangePrimary
                )
            }
        }
    }
}

@Composable
private fun FloatingCartBar(
    visible: Boolean,
    cartSize: Int,
    cartTotal: Double,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 12.dp) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(OrangeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ShoppingCart, null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("$cartSize item dipilih", fontSize = 13.sp, color = Color(0xFF888888))
                        Text(formatToRupiah(cartTotal), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                    }
                }
                Button(
                    onClick = onCartClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.White),
                    modifier = Modifier.height(44.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("Pesan Sekarang", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 14.sp, color = Color(0xFFBBBBBB))
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun getEmojiForCategory(kategori: String): String {
    return when {
        kategori.contains("Nasi", ignoreCase = true) -> "🍚"
        kategori.contains("Mie", ignoreCase = true) || kategori.contains("Noodle", ignoreCase = true) -> "🍜"
        kategori.contains("Ayam", ignoreCase = true) || kategori.contains("Chicken", ignoreCase = true) -> "🍗"
        kategori.contains("Snack", ignoreCase = true) || kategori.contains("Cemilan", ignoreCase = true) -> "🍟"
        kategori.contains("Minuman", ignoreCase = true) || kategori.contains("Drink", ignoreCase = true) -> "🥤"
        kategori.contains("Panas", ignoreCase = true) || kategori.contains("Hot", ignoreCase = true) -> "☕"
        kategori.contains("Dingin", ignoreCase = true) || kategori.contains("Cold", ignoreCase = true) || kategori.contains("Es", ignoreCase = true) -> "🧊"
        kategori.contains("Dessert", ignoreCase = true) || kategori.contains("Pencuci", ignoreCase = true) -> "🍰"
        kategori.contains("Utama", ignoreCase = true) || kategori.contains("Main", ignoreCase = true) -> "🍽️"
        kategori.contains("Paket", ignoreCase = true) -> "📦"
        else -> "🍽️"
    }
}

fun formatToRupiah(amount: Double): String {
    return "Rp ${String.format(Locale.getDefault(), "%,d", amount.toLong())}"
}

