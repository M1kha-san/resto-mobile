package com.example.restoapp.screens.user

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.restoapp.api.CustomerApiService
import com.example.restoapp.models.Meja
import com.example.restoapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DineInEntryScreen(navController: NavController) {
    var customerName by remember { mutableStateOf("") }
    var selectedTable by remember { mutableStateOf<Meja?>(null) }
    var showContent by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(1) }
    var tables by remember { mutableStateOf<List<Meja>>(emptyList()) }
    var isLoadingTables by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val refreshTables: () -> Unit = {
        scope.launch {
            val result = withContext(Dispatchers.IO) { CustomerApiService.getMejaWithActiveCheck() }
            result.onSuccess { mejaList ->
                tables = mejaList
                selectedTable?.let { selected ->
                    val updated = mejaList.find { it.mejaId == selected.mejaId }
                    if (updated != null && !updated.status.equals("Tersedia", ignoreCase = true)) {
                        selectedTable = null
                        Toast.makeText(context, "Meja ${selected.nomorMeja} sudah tidak tersedia", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        isLoadingTables = true
        val result = withContext(Dispatchers.IO) { CustomerApiService.getMejaWithActiveCheck() }
        result.fold(
            onSuccess = { tables = it },
            onFailure = { Toast.makeText(context, "Gagal memuat data meja: ${it.message}", Toast.LENGTH_SHORT).show() }
        )
        isLoadingTables = false
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
                .clip(CircleShape).background(SecondaryGold.copy(alpha = 0.2f)).blur(50.dp)
        )
        Box(
            modifier = Modifier.size(180.dp).align(Alignment.BottomEnd).offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape).background(PrimaryOrangeLight.copy(alpha = 0.2f)).blur(45.dp)
        )

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            // Bar atas dengan tombol kembali dan indikator langkah
            DineInTopBar(
                currentStep = currentStep,
                onBack = {
                    if (currentStep == 2) currentStep = 1
                    else navController.popBackStack()
                }
            )

            // Konten bergantian antara langkah 1 dan 2
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "stepTransition"
            ) { step ->
                when (step) {
                    1 -> DineInNameInputStep(
                        name = customerName,
                        onNameChange = { customerName = it },
                        onNext = { currentStep = 2 },
                        showContent = showContent
                    )
                    2 -> DineInTableSelectionStep(
                        tables = tables,
                        selectedTable = selectedTable,
                        onTableSelect = { selectedTable = it },
                        customerName = customerName,
                        onConfirm = {
                            selectedTable?.let { table ->
                                navController.navigate("menu/${customerName}/${table.mejaId}") {
                                    popUpTo("welcome") { inclusive = false }
                                }
                            }
                        },
                        showContent = showContent,
                        isLoading = isLoadingTables,
                        onRefresh = refreshTables
                    )
                }
            }
        }
    }
}

// ===== KOMPONEN BAR ATAS =====

@Composable
private fun DineInTopBar(currentStep: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White)
        }
        Spacer(modifier = Modifier.weight(1f))

        // Indikator langkah (1 = Nama, 2 = Meja)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepDot(step = 1, currentStep = currentStep, label = "Nama")
            Box(
                modifier = Modifier.width(30.dp).height(2.dp)
                    .background(
                        if (currentStep >= 2) Color.White else Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
            StepDot(step = 2, currentStep = currentStep, label = "Meja")
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(48.dp))
    }
}

// ===== INDIKATOR LANGKAH =====

@Composable
private fun StepDot(step: Int, currentStep: Int, label: String) {
    val isActive = currentStep >= step
    val scale by animateFloatAsState(
        targetValue = if (currentStep == step) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "stepScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.scale(scale).size(32.dp).clip(CircleShape)
                .background(if (isActive) Color.White else Color.White.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) PrimaryOrangeDark else Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = if (isActive) 1f else 0.5f),
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// ===== LANGKAH 1: INPUT NAMA =====

@Composable
private fun DineInNameInputStep(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
    showContent: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -50 }, animationSpec = tween(600))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Ikon dine in
                Box(
                    modifier = Modifier.size(100.dp).shadow(16.dp, CircleShape).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🍽️", fontSize = 48.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Dine In", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Silakan masukkan nama dan pilih meja",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Kartu form input nama
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Nama Anda") },
                        placeholder = { Text("Contoh: Aji Dwi Wahyu") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, null, tint = PrimaryOrange)
                        },
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

                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Pilih Meja", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ===== LANGKAH 2: PILIH MEJA =====

@Composable
private fun DineInTableSelectionStep(
    tables: List<Meja>,
    selectedTable: Meja?,
    onTableSelect: (Meja) -> Unit,
    customerName: String,
    onConfirm: () -> Unit,
    showContent: Boolean,
    isLoading: Boolean,
    onRefresh: (() -> Unit)? = null
) {
    // Auto-refresh data meja setiap 10 detik
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            onRefresh?.invoke()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        // Header sapaan
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -50 }, animationSpec = tween(600))
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Hai, $customerName!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pilih meja yang tersedia", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))

                Spacer(modifier = Modifier.height(12.dp))

                // Legenda warna status meja
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableLegendItem(color = SuccessGreen, text = "Tersedia")
                    TableLegendItem(color = ErrorRed, text = "Terisi")
                    TableLegendItem(color = Color(0xFFFF9800), text = "Dipesan")
                    TableLegendItem(color = SecondaryGold, text = "Dipilih")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid daftar meja
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(tables) { table ->
                        DineInTableCard(
                            table = table,
                            isSelected = selectedTable?.mejaId == table.mejaId,
                            onClick = { if (table.status.equals("Tersedia", ignoreCase = true)) onTableSelect(table) }
                        )
                    }
                }
            }
        }

        // Tombol konfirmasi (muncul setelah memilih meja)
        AnimatedVisibility(
            visible = selectedTable != null,
            enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 100 }) + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Meja ${selectedTable?.nomorMeja ?: ""}",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryOrangeDark
                            )
                            Text(
                                "Kapasitas ${selectedTable?.kapasitas ?: 0} orang",
                                fontSize = 14.sp, color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Konfirmasi & Pesan Menu", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===== KOMPONEN PENDUKUNG =====

/** Item legenda warna untuk status meja. */
@Composable
private fun TableLegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(text, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
    }
}

/** Kartu untuk satu meja di grid pemilihan meja. */
@Composable
private fun DineInTableCard(table: Meja, isSelected: Boolean, onClick: () -> Unit) {
    val isAvailable = table.status.equals("Tersedia", ignoreCase = true)
    val isTerisi = table.status.equals("Terisi", ignoreCase = true)
    val isDipesan = table.status.equals("Dipesan", ignoreCase = true)
    val isOccupied = isTerisi || isDipesan

    // Tentukan warna berdasarkan status meja
    val statusBadgeColor = when {
        isSelected -> SecondaryGold
        isAvailable -> SuccessGreen
        isTerisi -> ErrorRed
        isDipesan -> Color(0xFFFF9800)
        else -> Color.Gray
    }
    val iconBgColor = when {
        isSelected -> SecondaryGold.copy(alpha = 0.2f)
        isAvailable -> PrimaryOrangeLight.copy(alpha = 0.2f)
        isTerisi -> ErrorRed.copy(alpha = 0.1f)
        isDipesan -> Color(0xFFFF9800).copy(alpha = 0.1f)
        else -> Color.Gray.copy(alpha = 0.2f)
    }
    val iconTintColor = when {
        isSelected -> SecondaryGold
        isAvailable -> PrimaryOrange
        isTerisi -> ErrorRed
        isDipesan -> Color(0xFFFF9800)
        else -> Color.Gray
    }
    val statusLabel = when {
        isSelected -> "Dipilih ✓"
        isAvailable -> "Tersedia"
        isTerisi -> "Sedang Digunakan"
        isDipesan -> "Sudah Dipesan"
        else -> table.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(
                elevation = if (isSelected) 16.dp else if (isOccupied) 2.dp else 8.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(enabled = isAvailable, onClick = onClick)
            .then(
                if (isSelected) Modifier.border(3.dp, SecondaryGold, RoundedCornerShape(20.dp))
                else if (isOccupied) Modifier.border(1.dp, statusBadgeColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable || isSelected) Color.White else Color(0xFFF5F5F5).copy(alpha = 0.8f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Overlay transparan untuk meja tidak tersedia
            if (isOccupied) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.04f)))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                // Ikon meja
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.TableBar, null, modifier = Modifier.size(32.dp), tint = iconTintColor)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Meja ${table.nomorMeja}",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = if (isAvailable || isSelected) PrimaryOrangeDark else Color.Gray.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${table.kapasitas} orang",
                    fontSize = 14.sp,
                    color = if (isAvailable || isSelected) TextSecondary else Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Badge status
                Surface(shape = RoundedCornerShape(8.dp), color = statusBadgeColor) {
                    Text(
                        statusLabel,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

