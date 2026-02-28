package com.example.restoapp.screens.welcome

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.restoapp.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Halaman selamat datang aplikasi RestoApp.
 *
 * Menampilkan dua pilihan metode pemesanan:
 * - Dine In: makan di tempat (pilih meja)
 * - Take Away: bawa pulang (tanpa meja)
 */
@Composable
fun WelcomeScreen(navController: NavController) {
    var showContent by remember { mutableStateOf(false) }
    var showCards by remember { mutableStateOf(false) }

    // Efek melayang untuk logo
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // Tampilkan konten secara bertahap
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        delay(300)
        showCards = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryOrange, PrimaryOrangeDark, Color(0xFF2D1810))
                )
            )
    ) {
        // Elemen dekoratif latar belakang
        DecorationCircle(
            size = 250.dp,
            color = PrimaryOrangeLight.copy(alpha = 0.3f),
            offsetX = (-80).dp,
            offsetY = (-80).dp,
            blurRadius = 60.dp
        )
        DecorationCircle(
            size = 200.dp,
            color = SecondaryGold.copy(alpha = 0.3f),
            offsetX = 80.dp,
            offsetY = 150.dp,
            blurRadius = 50.dp,
            alignment = Alignment.TopEnd
        )
        DecorationCircle(
            size = 220.dp,
            color = PrimaryOrangeLight.copy(alpha = 0.2f),
            offsetX = (-50).dp,
            offsetY = 100.dp,
            blurRadius = 55.dp,
            alignment = Alignment.BottomStart
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Header dengan logo dan teks sambutan
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(800)) + slideInVertically(
                    initialOffsetY = { -100 },
                    animationSpec = tween(800, easing = EaseOutBack)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Logo dengan efek melayang
                    Box(
                        modifier = Modifier
                            .offset(y = floatOffset.dp)
                            .size(120.dp)
                            .shadow(24.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(listOf(SecondaryGold, SecondaryGoldDark))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🍽️", fontSize = 56.sp)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Selamat Datang",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nikmati pengalaman kuliner terbaik",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Judul pilihan metode pemesanan
            AnimatedVisibility(
                visible = showCards,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(600, delayMillis = 200)
                )
            ) {
                Text(
                    text = "Pilih Metode Pemesanan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // Kartu pilihan Dine In
            AnimatedVisibility(
                visible = showCards,
                enter = fadeIn(tween(600, delayMillis = 300)) + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(600, delayMillis = 300, easing = EaseOutBack)
                )
            ) {
                OrderMethodCard(
                    emoji = "🍽️",
                    title = "Dine In",
                    subtitle = "Makan di tempat",
                    description = "Pesan meja dan nikmati di restoran",
                    backgroundColor = SecondaryGold,
                    onClick = { navController.navigate("user_entry_dinein") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kartu pilihan Take Away
            AnimatedVisibility(
                visible = showCards,
                enter = fadeIn(tween(600, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(600, delayMillis = 400, easing = EaseOutBack)
                )
            ) {
                OrderMethodCard(
                    emoji = "🥡",
                    title = "Take Away",
                    subtitle = "Bawa pulang",
                    description = "Pesan makanan untuk dibawa",
                    backgroundColor = PrimaryOrangeLight,
                    onClick = { navController.navigate("user_entry_takeaway") }
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Komponen lingkaran dekoratif untuk latar belakang.
 * Memberikan efek visual gradient yang lembut.
 */
@Composable
private fun BoxScope.DecorationCircle(
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    blurRadius: androidx.compose.ui.unit.Dp,
    alignment: Alignment = Alignment.TopStart
) {
    Box(
        modifier = Modifier
            .size(size)
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .clip(CircleShape)
            .background(color)
            .blur(blurRadius)
    )
}

/**
 * Kartu pilihan metode pemesanan (Dine In / Take Away).
 */
@Composable
private fun OrderMethodCard(
    emoji: String,
    title: String,
    subtitle: String,
    description: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = backgroundColor.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikon emoji
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Keterangan
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

