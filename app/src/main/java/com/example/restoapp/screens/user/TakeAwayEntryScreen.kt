package com.example.restoapp.screens.user

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.restoapp.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Halaman entry Take Away.
 *
 * Pelanggan memasukkan nama dan nomor telepon, lalu
 * diarahkan ke halaman menu untuk memilih pesanan.
 * Tidak memerlukan pemilihan meja (mejaId = 0).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAwayEntryScreen(navController: NavController) {
    var customerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
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
        // Elemen dekoratif
        Box(
            modifier = Modifier.size(200.dp).offset(x = (-60).dp, y = (-60).dp)
                .clip(CircleShape).background(PrimaryOrangeLight.copy(alpha = 0.2f)).blur(50.dp)
        )
        Box(
            modifier = Modifier.size(180.dp).align(Alignment.BottomEnd).offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape).background(SecondaryGold.copy(alpha = 0.2f)).blur(45.dp)
        )

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            // Bar atas dengan tombol kembali
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // Konten utama
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Header dengan ikon dan judul
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -50 }, animationSpec = tween(600))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(100.dp).shadow(16.dp, CircleShape).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🥡", fontSize = 48.sp)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Take Away", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Pesan makanan untuk dibawa pulang",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Form input nama dan nomor telepon
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                        initialOffsetY = { 50 }, animationSpec = tween(600, delayMillis = 200)
                    )
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Input nama
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Nama Anda") },
                                placeholder = { Text("Contoh: Aji Dwi Wahyu") },
                                leadingIcon = { Icon(Icons.Filled.Person, null, tint = PrimaryOrange) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryOrange,
                                    unfocusedBorderColor = DividerColor,
                                    focusedLabelColor = PrimaryOrange,
                                    cursorColor = PrimaryOrange
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Input nomor telepon
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Nomor Telepon") },
                                placeholder = { Text("Contoh: 08123456789") },
                                leadingIcon = { Icon(Icons.Filled.Phone, null, tint = PrimaryOrange) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryOrange,
                                    unfocusedBorderColor = DividerColor,
                                    focusedLabelColor = PrimaryOrange,
                                    cursorColor = PrimaryOrange
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Tombol lanjut ke halaman menu
                            Button(
                                onClick = {
                                    if (customerName.isNotBlank() && phoneNumber.isNotBlank()) {
                                        // mejaId = 0 artinya take away (tanpa meja)
                                        navController.navigate("menu/${customerName}/0") {
                                            popUpTo("welcome") { inclusive = false }
                                        }
                                    } else {
                                        Toast.makeText(context, "Mohon lengkapi data terlebih dahulu", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                enabled = customerName.isNotBlank() && phoneNumber.isNotBlank()
                            ) {
                                Text("Lanjut Pesan Menu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

