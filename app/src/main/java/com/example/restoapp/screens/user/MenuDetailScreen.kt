package com.example.restoapp.screens.user

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.restoapp.api.CustomerApiService
import com.example.restoapp.models.Menu
import com.example.restoapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDetailScreen(
    navController: NavController,
    menu: Menu,
    onAddToCart: (Menu, Int, String) -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    var note by remember { mutableStateOf("") }
    var showNote by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    var recommendedMenus by remember { mutableStateOf<List<Menu>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        showContent = true
        val result = withContext(Dispatchers.IO) { CustomerApiService.getMenu() }
        result.onSuccess { menus ->
            recommendedMenus = menus.filter {
                it.menuId != menu.menuId && it.ketersediaan &&
                        it.kategori.equals(menu.kategori, ignoreCase = true)
            }.take(6)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Konten yang bisa di-scroll
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Bagian hero (gambar/emoji besar)
            DetailHeroSection(menu = menu, onBack = { navController.popBackStack() })

            // Kartu konten utama
            DetailContentCard(
                menu = menu,
                quantity = quantity,
                onQuantityChange = { quantity = it },
                note = note,
                onNoteChange = { note = it },
                showNote = showNote,
                onToggleNote = { showNote = !showNote },
                recommendedMenus = recommendedMenus,
                onBackToMenu = { navController.popBackStack() },
                context = context
            )
        }

        // Tombol tambah ke keranjang (tetap di bawah)
        DetailBottomBar(
            visible = showContent,
            menu = menu,
            quantity = quantity,
            onAddToCart = {
                if (menu.ketersediaan) {
                    onAddToCart(menu, quantity, note)
                    Toast.makeText(context, "${menu.namaMenu} ditambahkan ke keranjang", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Menu ini sedang tidak tersedia", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DetailHeroSection(menu: Menu, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color(0xFF1C1C1E))) {
        // Gradient overlay lembut
        Box(
            modifier = Modifier.fillMaxSize().background(
                brush = Brush.radialGradient(
                    colors = listOf(OrangePrimary.copy(alpha = 0.15f), Color.Transparent)
                )
            )
        )

        // Emoji makanan besar di tengah
        Text(
            text = getEmojiForCategory(menu.kategori),
            fontSize = 120.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Tombol kembali (kiri atas)
        Box(
            modifier = Modifier.padding(top = 44.dp, start = 16.dp).size(40.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        // Tombol favorit (kanan atas)
        Box(
            modifier = Modifier.padding(top = 44.dp, end = 16.dp).size(40.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.FavoriteBorder, "Favorit", tint = OrangePrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun DetailContentCard(
    menu: Menu,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    showNote: Boolean,
    onToggleNote: () -> Unit,
    recommendedMenus: List<Menu>,
    onBackToMenu: () -> Unit,
    context: android.content.Context
) {
    Surface(
        modifier = Modifier.fillMaxWidth().offset(y = (-20).dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
            // Baris nama menu dan pengatur jumlah
            MenuNameAndQuantityRow(menu, quantity, onQuantityChange)

            Spacer(modifier = Modifier.height(14.dp))

            // Baris rating dan badge stok
            RatingAndStockRow(menu)

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(20.dp))

            // Deskripsi menu (bisa di-expand)
            MenuDescription(menu)

            Spacer(modifier = Modifier.height(20.dp))

            // Catatan khusus (bisa di-expand)
            SpecialNoteSection(note, onNoteChange, showNote, onToggleNote)

            // Rekomendasi menu sejenis
            if (recommendedMenus.isNotEmpty()) {
                RecommendedSection(recommendedMenus, onBackToMenu, context)
            }

            // Ruang kosong untuk tombol di bawah
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/** Baris nama menu dengan pengatur jumlah pesanan. */
@Composable
private fun MenuNameAndQuantityRow(menu: Menu, quantity: Int, onQuantityChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(menu.namaMenu, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E), lineHeight = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(menu.kategori, fontSize = 14.sp, color = Color(0xFF8E8E93))
        }

        // Pengatur jumlah (- angka +)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuantityButton(icon = Icons.Filled.Remove, enabled = quantity > 1, isPrimary = false) {
                if (quantity > 1) onQuantityChange(quantity - 1)
            }
            Text(
                String.format(Locale.getDefault(), "%02d", quantity),
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E),
                modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center
            )
            QuantityButton(icon = Icons.Filled.Add, enabled = menu.ketersediaan, isPrimary = true) {
                if (menu.ketersediaan) onQuantityChange(quantity + 1)
            }
        }
    }
}

/** Baris rating dan badge status ketersediaan stok. */
@Composable
private fun RatingAndStockRow(menu: Menu) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Filled.Star, null, tint = GoldStar, modifier = Modifier.size(18.dp))
        Text("4.9", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1C1E))
        Text("(355 Review)", fontSize = 13.sp, color = Color(0xFF8E8E93))
        Spacer(modifier = Modifier.weight(1f))

        if (menu.ketersediaan) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE8F5E9)) {
                Text(
                    "Tersedia · Stok ${menu.stok}",
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        } else {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFEBEE)) {
                Text(
                    "Habis", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFC62828),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** Deskripsi menu yang bisa di-expand jika terlalu panjang. */
@Composable
private fun MenuDescription(menu: Menu) {
    val desc = menu.deskripsi?.takeIf { it.isNotEmpty() }
        ?: "Menu lezat dari kategori ${menu.kategori} yang disajikan dengan bahan-bahan berkualitas tinggi dan cita rasa yang autentik. Cocok untuk menikmati waktu makan bersama keluarga."

    var isExpanded by remember { mutableStateOf(false) }

    Text(
        desc, fontSize = 14.sp, color = Color(0xFF6B6B6B), lineHeight = 22.sp,
        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
        overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
    )
    if (desc.length > 100) {
        Text(
            if (isExpanded) "Lebih sedikit" else "Lihat Selengkapnya",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OrangePrimary,
            modifier = Modifier.padding(top = 4.dp).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { isExpanded = !isExpanded }
        )
    }
}

/** Bagian catatan khusus yang bisa di-expand/collapse. */
@Composable
private fun SpecialNoteSection(
    note: String,
    onNoteChange: (String) -> Unit,
    showNote: Boolean,
    onToggleNote: () -> Unit
) {
    // Tombol toggle catatan
    Row(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { onToggleNote() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.EditNote, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
            Text("Catatan Khusus", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1C1E))
        }
        Icon(
            if (showNote) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            null, tint = Color(0xFF8E8E93), modifier = Modifier.size(20.dp)
        )
    }

    // Kolom input catatan (muncul saat di-expand)
    AnimatedVisibility(visible = showNote) {
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth().height(100.dp),
                placeholder = { Text("Contoh: tidak pedas, tanpa bawang...", color = Color(0xFFBDBDBD), fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA),
                    cursorColor = OrangePrimary
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
    }
}

/** Bagian rekomendasi menu sejenis (scroll horizontal). */
@Composable
private fun RecommendedSection(
    menus: List<Menu>,
    onBackToMenu: () -> Unit,
    context: android.content.Context
) {
    Spacer(modifier = Modifier.height(28.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Menu Lainnya", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
        Text(
            "Lihat Semua", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OrangePrimary,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { onBackToMenu() }
        )
    }
    Spacer(modifier = Modifier.height(14.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 4.dp)) {
        items(menus) { rec ->
            RecommendedMenuCard(menu = rec) {
                Toast.makeText(context, rec.namaMenu, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun DetailBottomBar(
    visible: Boolean,
    menu: Menu,
    quantity: Int,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Harga", fontSize = 12.sp, color = Color(0xFF8E8E93))
                    Text(formatToRupiah(menu.harga * quantity), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                }
                Button(
                    onClick = onAddToCart,
                    modifier = Modifier.height(52.dp).widthIn(min = 180.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (menu.ketersediaan) OrangePrimary else Color(0xFFBDBDBD),
                        contentColor = Color.White
                    ),
                    enabled = menu.ketersediaan,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambah ke Keranjang", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuantityButton(icon: ImageVector, enabled: Boolean, isPrimary: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isPrimary -> OrangePrimary
                    enabled -> OrangeSoft
                    else -> Color(0xFFF0F0F0)
                }
            )
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = true)) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null,
            tint = when {
                isPrimary -> Color.White
                enabled -> OrangePrimary
                else -> Color(0xFFBDBDBD)
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Kartu menu kecil untuk rekomendasi (scroll horizontal). */
@Composable
private fun RecommendedMenuCard(menu: Menu, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(90.dp).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(90.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(getEmojiForCategory(menu.kategori), fontSize = 44.sp, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            menu.namaMenu, fontSize = 11.sp, color = Color(0xFF1C1C1E),
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 14.sp
        )
    }
}

