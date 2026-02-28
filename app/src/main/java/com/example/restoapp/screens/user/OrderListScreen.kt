package com.example.restoapp.screens.user

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.restoapp.models.CartItem
import com.example.restoapp.models.MetodePesanan
import com.example.restoapp.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    navController: NavController,
    customerName: String,
    cart: List<CartItem>,
    onUpdateQuantity: (CartItem, Int) -> Unit,
    onUpdateNote: (CartItem, String) -> Unit,
    onCheckout: () -> Unit,
    onClearCart: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var isCheckingOut by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    val cartItemCount = remember(cart) { cart.sumOf { it.quantity } }
    val totalPrice = remember(cart) { cart.sumOf { it.subtotal } }

    Box(modifier = Modifier.fillMaxSize().background(CreamBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Bar atas: judul, jumlah item, tombol hapus semua
            OrderTopBar(
                cartSize = cart.size,
                cartItemCount = cartItemCount,
                onBack = { navController.popBackStack() },
                onClearCart = { showClearDialog = true }
            )

            // Konten utama
            if (cart.isEmpty()) {
                // Tampilan keranjang kosong
                EmptyCartView(showContent, onViewMenu = { navController.popBackStack() })
            } else {
                // Daftar item dalam keranjang
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 200.dp)
                ) {
                    // Kartu ringkasan pelanggan
                    item {
                        AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })) {
                            CustomerSummaryCard(customerName, cartItemCount)
                        }
                    }

                    // Item-item keranjang
                    items(cart, key = { it.menu.menuId }) { item ->
                        AnimatedVisibility(visible = showContent, enter = fadeIn(tween(300)) + slideInHorizontally()) {
                            OrderItemCard(item = item, onUpdateQuantity = onUpdateQuantity, onUpdateNote = onUpdateNote)
                        }
                    }
                }
            }
        }

        // Bar checkout di bagian bawah
        if (cart.isNotEmpty()) {
            CheckoutBar(
                visible = showContent,
                cartItemCount = cartItemCount,
                totalPrice = totalPrice,
                isCheckingOut = isCheckingOut,
                onCheckout = {
                    isCheckingOut = true
                    onCheckout()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Dialog konfirmasi hapus semua
    if (showClearDialog) {
        ClearCartDialog(
            onConfirm = { onClearCart(); showClearDialog = false },
            onDismiss = { showClearDialog = false }
        )
    }
}

@Composable
private fun OrderTopBar(
    cartSize: Int,
    cartItemCount: Int,
    onBack: () -> Unit,
    onClearCart: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol kembali
                Surface(modifier = Modifier.size(44.dp).clickable { onBack() }, shape = CircleShape, color = OrangeSoft) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = OrangePrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.ShoppingCart, null, tint = OrangePrimary, modifier = Modifier.size(28.dp))
                        Text("Keranjang", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarmGray)
                    }
                    if (cartSize > 0) {
                        Text("$cartItemCount item dalam keranjang", fontSize = 14.sp, color = TextSecondary)
                    }
                }
                // Tombol hapus semua
                if (cartSize > 0) {
                    Surface(modifier = Modifier.size(44.dp).clickable { onClearCart() }, shape = CircleShape, color = ErrorRed.copy(alpha = 0.1f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.DeleteSweep, "Hapus Semua", tint = ErrorRed, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptyCartView(showContent: Boolean, onViewMenu: () -> Unit) {
    AnimatedVisibility(visible = showContent, enter = fadeIn() + scaleIn(initialScale = 0.8f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(modifier = Modifier.size(140.dp), shape = CircleShape, color = OrangeSoft) {
                Box(contentAlignment = Alignment.Center) { Text("🛒", fontSize = 72.sp) }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text("Keranjang Kosong", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = WarmGray)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Belum ada menu yang ditambahkan.\nAyo mulai pesan menu favoritmu!",
                fontSize = 16.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = onViewMenu,
                modifier = Modifier.fillMaxWidth(0.75f).height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Filled.Restaurant, null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Lihat Menu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CustomerSummaryCard(customerName: String, cartItemCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OrangePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Pesanan untuk", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                Text(customerName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
                Text("$cartItemCount items", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OrangePrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun OrderItemCard(
    item: CartItem,
    onUpdateQuantity: (CartItem, Int) -> Unit,
    onUpdateNote: (CartItem, String) -> Unit
) {
    var showNoteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Emoji menu
                Box(
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
                        .background(brush = Brush.linearGradient(colors = listOf(OrangeSoft, OrangeAccent.copy(alpha = 0.3f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(getEmojiForCategory(item.menu.kategori), fontSize = 44.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Badge tipe pesanan (Dine In / Take Away)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.metodePesanan == MetodePesanan.TAKE_AWAY) SecondaryGold.copy(alpha = 0.2f) else OrangeSoft
                    ) {
                        Text(
                            item.metodePesanan.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (item.metodePesanan == MetodePesanan.TAKE_AWAY) SecondaryGold else OrangePrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.menu.namaMenu, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WarmGray, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(formatToRupiah(item.menu.harga), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OrangePrimary)

                    // Tampilkan catatan jika ada
                    if (item.catatan.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = WarmGray.copy(alpha = 0.08f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Notes, null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                Text(item.catatan, fontSize = 12.sp, color = TextSecondary, fontStyle = FontStyle.Italic, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Baris kontrol: tombol catatan, pengatur jumlah, subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol edit catatan
                OutlinedButton(
                    onClick = { showNoteDialog = true },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, OrangePrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangePrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Icon(if (item.catatan.isEmpty()) Icons.Filled.AddComment else Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (item.catatan.isEmpty()) "Catatan" else "Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Pengatur jumlah item
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Tombol kurangi / hapus
                    Surface(
                        modifier = Modifier.size(42.dp).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) {
                            if (item.quantity > 1) onUpdateQuantity(item, item.quantity - 1) else onUpdateQuantity(item, 0)
                        },
                        shape = CircleShape,
                        color = if (item.quantity > 1) OrangeSoft else ErrorRed.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (item.quantity > 1) Icons.Filled.Remove else Icons.Filled.Delete,
                                if (item.quantity > 1) "Kurangi" else "Hapus",
                                tint = if (item.quantity > 1) OrangePrimary else ErrorRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Jumlah item
                    Text(item.quantity.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WarmGray, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)

                    // Tombol tambah
                    Surface(
                        modifier = Modifier.size(42.dp).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) { onUpdateQuantity(item, item.quantity + 1) },
                        shape = CircleShape, color = OrangePrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, "Tambah", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Subtotal per item
                Column(horizontalAlignment = Alignment.End) {
                    Text("Subtotal", fontSize = 12.sp, color = TextSecondary)
                    Text(formatToRupiah(item.subtotal), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                }
            }
        }
    }

    // Dialog edit catatan
    if (showNoteDialog) {
        EditNoteDialog(
            itemName = item.menu.namaMenu,
            initialNote = item.catatan,
            onSave = { newNote -> onUpdateNote(item, newNote); showNoteDialog = false },
            onDismiss = { showNoteDialog = false }
        )
    }
}

@Composable
private fun CheckoutBar(
    visible: Boolean,
    cartItemCount: Int,
    totalPrice: Double,
    isCheckingOut: Boolean,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 24.dp,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                // Subtotal
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Subtotal ($cartItemCount item)", fontSize = 15.sp, color = TextSecondary)
                    Text(formatToRupiah(totalPrice), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = WarmGray)
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Total pembayaran
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total Pembayaran", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WarmGray)
                    Text(formatToRupiah(totalPrice), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = OrangePrimary)
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Tombol pesan
                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = !isCheckingOut,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
                ) {
                    if (isCheckingOut) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = Color.White, strokeWidth = 3.dp)
                    } else {
                        Icon(Icons.Filled.Payment, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Pesan Sekarang", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditNoteDialog(
    itemName: String,
    initialNote: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        icon = {
            Surface(shape = CircleShape, color = OrangeSoft, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Edit, null, tint = OrangePrimary, modifier = Modifier.size(32.dp))
                }
            }
        },
        title = {
            Text("Catatan untuk", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WarmGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column {
                Text(itemName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OrangePrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    placeholder = { Text("Contoh: Tidak pedas, tanpa bawang...", color = TextSecondary.copy(alpha = 0.6f)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary, unfocusedBorderColor = DividerColor,
                        focusedContainerColor = OrangeSoft.copy(alpha = 0.3f), unfocusedContainerColor = CreamBackground
                    ),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(noteText) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, DividerColor)) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}

/** Dialog konfirmasi hapus semua item dari keranjang. */
@Composable
private fun ClearCartDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        icon = {
            Surface(shape = CircleShape, color = ErrorRed.copy(alpha = 0.1f), modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.DeleteForever, null, tint = ErrorRed, modifier = Modifier.size(40.dp))
                }
            }
        },
        title = {
            Text("Hapus Semua Pesanan?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WarmGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text("Semua item dalam keranjang akan dihapus.", fontSize = 15.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp)
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hapus Semua", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, DividerColor)) {
                Text("Batal", color = TextSecondary)
            }
        }
    )
}

