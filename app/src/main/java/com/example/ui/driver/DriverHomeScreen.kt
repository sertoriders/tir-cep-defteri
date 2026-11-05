package com.example.ui.driver

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.data.repository.FleetRepository
import com.example.service.TrackingService
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDarkest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    userProfile: UserProfile,
    repository: FleetRepository,
    onSignOut: () -> Unit,
    onNavigateToAdmin: (() -> Unit)? = null // Admin kendi aracını kullanırken panele dönebilsin
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val vehicles by repository.observeVehicles().collectAsState(initial = emptyList())
    // Şoföre atanmış aracı bul; eğer atanmamışsa ilk aracı göster
    val myVehicle = vehicles.firstOrNull { it.id == userProfile.assignedVehicleId }
        ?: vehicles.firstOrNull { it.assignedDriverId == userProfile.userId }
        ?: vehicles.firstOrNull()

    val trips by repository.observeTrips().collectAsState(initial = emptyList())
    val activeTrip = trips.firstOrNull { it.vehicleId == myVehicle?.id && it.status == "ACTIVE" }

    var showStartLoadedDialog by remember { mutableStateOf(false) }
    var showStartEmptyDialog by remember { mutableStateOf(false) }
    var showEndTripDialog by remember { mutableStateOf(false) }
    var showFuelDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showCorrectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TIR CEP DEFTERİ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "Şoför: ${userProfile.name}",
                            fontSize = 12.sp,
                            color = CyanAccent
                        )
                    }
                },
                actions = {
                    if (onNavigateToAdmin != null) {
                        OutlinedButton(
                            onClick = onNavigateToAdmin,
                            modifier = Modifier.padding(end = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                        ) {
                            Text("Yönetici Paneli", fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Çıkış Yap",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDarkest)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NavyBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Sürüş Algılandı Uyarısı (İstirahat veya Sefer Kayıtsız Hareket)
            if (myVehicle != null && myVehicle.unrecordedDriveAlert) {
                UnrecordedDriveAlertBanner(
                    vehicle = myVehicle,
                    onStartLoadedClick = { showStartLoadedDialog = true },
                    onStartEmptyClick = { showStartEmptyDialog = true },
                    onDismissClick = {
                        coroutineScope.launch {
                            repository.setUnrecordedDriveAlert(myVehicle.id, false)
                            snackbarHostState.showSnackbar("Uyarı kapatıldı.")
                        }
                    }
                )
            }

            // 1. Araç Bilgi & Canlı Durum Kartı
            if (myVehicle != null) {
                VehicleStatusCard(vehicle = myVehicle, activeTrip = activeTrip)

                // İstirahat Modu (Dinlenme & Mola) Kartı
                DriverRestModeCard(
                    vehicle = myVehicle,
                    isTripActive = activeTrip != null,
                    onToggleRestMode = { startRest ->
                        coroutineScope.launch {
                            try {
                                repository.setVehicleRestMode(myVehicle.id, startRest)
                                if (startRest) {
                                    snackbarHostState.showSnackbar("🛌 İstirahat modu başlatıldı. İyi dinlenmeler!")
                                } else {
                                    snackbarHostState.showSnackbar("İstirahat modu sonlandırıldı. Sefere hazırsınız.")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                            }
                        }
                    }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)
                ) {
                    Text(
                        text = "Araç verileri yükleniyor veya atanmış araç bulunamadı.",
                        color = PureWhite,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 2. Canlı GPS Güzergâh Takibi Uyarısı (Açıkça bildirim)
            GpsTrackingNoticeCard(isTracking = activeTrip != null)

            // 3. Şoför İşlem Butonları (Sadece istenen 5 işlem)
            Text(
                text = "SEFER VE SAHA İŞLEMLERİ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                letterSpacing = 1.sp
            )

            // İşlem 1: Yükü aldım (Dolu Sefer)
            Button(
                onClick = { showStartLoadedDialog = true },
                enabled = activeTrip == null && myVehicle != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_load_trip"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusSuccess,
                    contentColor = PureWhite,
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color(0xFF64748B)
                )
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("1. Yükü Aldım (Dolu Sefer Başlat)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // İşlem 2: Boş seyir başlat
            Button(
                onClick = { showStartEmptyDialog = true },
                enabled = activeTrip == null && myVehicle != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_empty_trip"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueAccent,
                    contentColor = PureWhite,
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color(0xFF64748B)
                )
            ) {
                Icon(Icons.Default.Route, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("2. Boş Seyir Başlat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // İşlem 3: Seferi bitir
            Button(
                onClick = { showEndTripDialog = true },
                enabled = activeTrip != null && myVehicle != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_end_trip"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusDanger,
                    contentColor = PureWhite,
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color(0xFF64748B)
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("3. Seferi Bitir (Teslimat Yapıldı)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // İşlem 4: Yakıt girişi
                Button(
                    onClick = { showFuelDialog = true },
                    enabled = myVehicle != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .testTag("btn_fuel_entry"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = NavyDarkest
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("4. Yakıt Girişi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // İşlem 5: Masraf girişi
                Button(
                    onClick = { showExpenseDialog = true },
                    enabled = myVehicle != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .testTag("btn_expense_entry"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = NavyDarkest
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("5. Masraf Girişi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Düzeltme Talebi Gönder Butonu (Şoför doğrudan silemez/değiştiremez)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Kayıt Değişiklik Politikası",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = "Sistem güvenliği gereği girilen sefer, yakıt ve masraf fişleri şoför tarafından doğrudan silinemez. Düzeltme talebi oluşturabilirsiniz.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    OutlinedButton(
                        onClick = { showCorrectionDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Düzeltme Talebi Gönder", fontSize = 13.sp)
                    }
                }
            }

            // Sürüş Algılama & Bildirim Test Butonu (Simülasyon / Test İçin)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sürüş Algılama & Bildirim Testi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                    Text(
                        text = "İstirahat modu dışında veya sefer kayıtsız araç hareket ettiğinde gönderilen telefon bildirimini ve ekran uyarısını test edin.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            if (myVehicle != null) {
                                coroutineScope.launch {
                                    TrackingService.sendUnrecordedDriveNotification(context, myVehicle.plate)
                                    repository.setUnrecordedDriveAlert(myVehicle.id, true)
                                    snackbarHostState.showSnackbar("⚠️ Sürüş algılandı bildirimi gönderildi ve uyarı açıldı!")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_simulate_drive_alert"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🧪 Sürüş Algıla & Bildirim Gönder (Test)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Dialoglar
    if (showStartLoadedDialog && myVehicle != null) {
        StartTripDialog(
            tripType = "LOADED",
            vehicle = myVehicle,
            onDismiss = { showStartLoadedDialog = false },
            onConfirm = { origin, destination, startKm, price, pType, pDue ->
                showStartLoadedDialog = false
                coroutineScope.launch {
                    try {
                        val tripId = repository.startTrip(
                            vehicleId = myVehicle.id,
                            tripType = "LOADED",
                            origin = origin,
                            destination = destination,
                            startKm = startKm,
                            freightPrice = price,
                            paymentType = pType,
                            paymentDueDate = pDue,
                            startLat = myVehicle.lastLat,
                            startLng = myVehicle.lastLng
                        )
                        // Arka plan GPS takibini başlat
                        TrackingService.start(context, myVehicle.id, myVehicle.plate)
                        snackbarHostState.showSnackbar("Dolu sefer başlatıldı: $tripId. GPS takibi aktif!")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                    }
                }
            }
        )
    }

    if (showStartEmptyDialog && myVehicle != null) {
        StartTripDialog(
            tripType = "EMPTY",
            vehicle = myVehicle,
            onDismiss = { showStartEmptyDialog = false },
            onConfirm = { origin, destination, startKm, _, _, _ ->
                showStartEmptyDialog = false
                coroutineScope.launch {
                    try {
                        val tripId = repository.startTrip(
                            vehicleId = myVehicle.id,
                            tripType = "EMPTY",
                            origin = origin,
                            destination = destination,
                            startKm = startKm,
                            startLat = myVehicle.lastLat,
                            startLng = myVehicle.lastLng
                        )
                        TrackingService.start(context, myVehicle.id, myVehicle.plate)
                        snackbarHostState.showSnackbar("Boş seyir başlatıldı: $tripId. GPS takibi aktif!")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                    }
                }
            }
        )
    }

    if (showEndTripDialog && activeTrip != null && myVehicle != null) {
        EndTripDialog(
            trip = activeTrip,
            vehicle = myVehicle,
            onDismiss = { showEndTripDialog = false },
            onConfirm = { endKm ->
                showEndTripDialog = false
                coroutineScope.launch {
                    try {
                        repository.endTrip(
                            tripId = activeTrip.id,
                            vehicleId = myVehicle.id,
                            endKm = endKm,
                            endLat = myVehicle.lastLat,
                            endLng = myVehicle.lastLng
                        )
                        TrackingService.stop(context)
                        snackbarHostState.showSnackbar("Sefer başarıyla tamamlandı ve kapatıldı.")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                    }
                }
            }
        )
    }

    if (showFuelDialog && myVehicle != null) {
        FuelEntryDialog(
            vehicle = myVehicle,
            activeTrip = activeTrip,
            onDismiss = { showFuelDialog = false },
            onConfirm = { liters, price, receiptUrl ->
                showFuelDialog = false
                coroutineScope.launch {
                    try {
                        repository.addFuelEntry(
                            vehicleId = myVehicle.id,
                            tripId = activeTrip?.id ?: "",
                            liters = liters,
                            pricePerLiter = price,
                            locationAddress = myVehicle.lastLocationAddress,
                            latitude = myVehicle.lastLat,
                            longitude = myVehicle.lastLng,
                            receiptUrl = receiptUrl
                        )
                        snackbarHostState.showSnackbar("Yakıt girişi (${liters}L, ${(liters * price).toInt()} TL) kaydedildi.")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                    }
                }
            }
        )
    }

    if (showExpenseDialog && myVehicle != null) {
        ExpenseEntryDialog(
            vehicle = myVehicle,
            activeTrip = activeTrip,
            onDismiss = { showExpenseDialog = false },
            onConfirm = { category, amount, description, receiptUrl ->
                showExpenseDialog = false
                coroutineScope.launch {
                    try {
                        repository.addExpenseEntry(
                            vehicleId = myVehicle.id,
                            tripId = activeTrip?.id ?: "",
                            category = category,
                            amount = amount,
                            description = description,
                            locationAddress = myVehicle.lastLocationAddress,
                            latitude = myVehicle.lastLat,
                            longitude = myVehicle.lastLng,
                            receiptUrl = receiptUrl
                        )
                        snackbarHostState.showSnackbar("$category masrafı (${amount.toInt()} TL) kaydedildi.")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                    }
                }
            }
        )
    }

    if (showCorrectionDialog && myVehicle != null) {
        CorrectionRequestDialog(
            vehicleId = myVehicle.id,
            onDismiss = { showCorrectionDialog = false },
            onConfirm = { targetType, targetId, details ->
                showCorrectionDialog = false
                coroutineScope.launch {
                    try {
                        repository.submitCorrectionRequest(
                            targetType = targetType,
                            targetId = targetId,
                            vehicleId = myVehicle.id,
                            requestedChanges = details
                        )
                        snackbarHostState.showSnackbar("Düzeltme talebiniz yöneticiye iletildi.")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                    }
                }
            }
        )
    }
}

@Composable
fun VehicleStatusCard(vehicle: Vehicle, activeTrip: Trip?) {
    val fuelPercent = (vehicle.currentFuelLiters / vehicle.tankCapacity).coerceIn(0.0, 1.0).toFloat()
    val isLoaded = vehicle.status == "LOADED"
    val consumption = if (isLoaded) vehicle.avgConsumptionLoaded else vehicle.avgConsumptionEmpty
    val estimatedRangeKm = (vehicle.currentFuelLiters / (consumption / 100.0)).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vehicle.plate,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PureWhite
                    )
                    Text(
                        text = vehicle.name,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Durum Rozeti
                val (statusText, statusColor) = when {
                    vehicle.status == "RESTING" || vehicle.isResting -> "İSTİRAHAT / DİNLENMEDE" to Color(0xFF8B5CF6)
                    vehicle.status == "LOADED" -> "YÜKLÜ SEFERDE" to StatusSuccess
                    vehicle.status == "EMPTY" -> "BOŞ SEYİRDE" to BlueAccent
                    vehicle.status == "MAINTENANCE" -> "BAKIMDA" to StatusWarning
                    else -> "GARAJDA / BOŞTA" to Color(0xFF64748B)
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Kilometre & Yakıt Göstergesi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Toplam Sayaç", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = "${String.format(Locale.getDefault(), "%,.0f", vehicle.odometerKm)} km",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Tahmini Kalan Menzil", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = "~$estimatedRangeKm km (${consumption.toInt()}L/100km)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Depo Barı
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Depo Durumu: ${vehicle.currentFuelLiters.toInt()} / ${vehicle.tankCapacity.toInt()} Litre",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "%${(fuelPercent * 100).toInt()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (fuelPercent < 0.2f) StatusDanger else CyanAccent
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fuelPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (fuelPercent < 0.2f) StatusDanger else CyanAccent,
                    trackColor = NavySurface
                )
            }

            // Aktif Sefer Bilgisi
            if (activeTrip != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySurface, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aktif Sefer: ${activeTrip.tripNumber}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                        Text(
                            text = "${activeTrip.origin} ➔ ${activeTrip.destination}",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "Başlangıç: ${activeTrip.startKm.toInt()} km | GPS ile gidilen: ${String.format(Locale.getDefault(), "%.1f km", activeTrip.gpsRouteKm)}",
                            fontSize = 11.sp,
                            color = CyanAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GpsTrackingNoticeCard(isTracking: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) Color(0xFF0F3B2E) else NavySurface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isTracking) StatusSuccess else Color(0xFF64748B))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isTracking) "GPS CANLI TAKİP AKTİF" else "GPS Takibi Beklemede",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTracking) StatusSuccess else Color(0xFF94A3B8)
                )
                Text(
                    text = if (isTracking)
                        "Yasal Bilgilendirme: Seferiniz boyunca konumunuz 15 saniyede bir merkez ofise aktarılmaktadır. Ekran kapalıyken de çalışır."
                    else
                        "Sefer başlattığınızda arka plan servisimiz devreye girecektir.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
fun UnrecordedDriveAlertBanner(
    vehicle: Vehicle,
    onStartLoadedClick: () -> Unit,
    onStartEmptyClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unrecorded_drive_alert_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "⚠️ SÜRÜŞ ALGILANDI: SEFER KAYDI OLUŞTURUN!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite
                    )
                    Text(
                        text = "Araç hareket halinde (${vehicle.plate}) ancak aktif sefer yok!",
                        fontSize = 11.sp,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "İstirahat veya garaj modu dışında sürüş algılandı. Sayac-GPS farkı uyarısı veya filo cezası oluşmaması için lütfen sefer kaydı oluşturun:",
                fontSize = 12.sp,
                color = PureWhite,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartLoadedClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_alert_start_loaded"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess, contentColor = PureWhite)
                ) {
                    Text("1. Yükü Aldım", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onStartEmptyClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_alert_start_empty"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent, contentColor = PureWhite)
                ) {
                    Text("2. Boş Seyir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onDismissClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_alert_dismiss"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
            ) {
                Text("Park Halindeyim / Yanlış Alarm (Kapat)", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun DriverRestModeCard(
    vehicle: Vehicle,
    isTripActive: Boolean,
    onToggleRestMode: (Boolean) -> Unit
) {
    val isResting = vehicle.isResting || vehicle.status == "RESTING"

    // Dinlenme süresi sayacı
    var elapsedSeconds by remember(vehicle.restStartTime) {
        val startMs = vehicle.restStartTime?.toDate()?.time ?: System.currentTimeMillis()
        mutableStateOf(((System.currentTimeMillis() - startMs) / 1000).coerceAtLeast(0L))
    }

    LaunchedEffect(isResting, vehicle.restStartTime) {
        if (isResting) {
            val startMs = vehicle.restStartTime?.toDate()?.time ?: System.currentTimeMillis()
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - startMs) / 1000).coerceAtLeast(0L)
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val formattedDuration = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("driver_rest_mode_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isResting) Color(0xFF2E1065) else NavyCard
        ),
        border = if (isResting) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7)) else null,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isResting) Color(0xFFA855F7) else NavySurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isResting) Icons.Default.CheckCircle else Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = if (isResting) PureWhite else CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isResting) "İSTİRAHAT MODU (MOLA AKTİF)" else "İSTİRAHAT MODU (DİNLENME)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isResting) Color(0xFFE9D5FF) else PureWhite
                        )
                        Text(
                            text = if (isResting) "Takograf mola kaydı işleniyor" else "Mola & konaklama zamanları",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                if (isResting) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFA855F7).copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "MOLADA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC084FC)
                        )
                    }
                }
            }

            if (isResting) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1B4B), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Geçen İstirahat Süresi", fontSize = 11.sp, color = Color(0xFFC4B5FD))
                        Text(
                            text = formattedDuration,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite,
                            letterSpacing = 1.sp
                        )
                    }
                    val startTimeStr = vehicle.restStartTime?.toDate()?.let {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                    } ?: "--:--"
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Başlangıç Saati", fontSize = 11.sp, color = Color(0xFFC4B5FD))
                        Text(text = startTimeStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9D5FF))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ℹ️ Dinlenme esnasında araç hareket ederse otomatik sesli bildirim ve sefer uyarısı tetiklenir.",
                    fontSize = 11.sp,
                    color = Color(0xFFDDD6FE)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onToggleRestMode(false) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_end_rest_mode"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA855F7),
                        contentColor = PureWhite
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("İstirahatı Bitir (Yola Çıkış)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Mola verdiğinizde veya yatıya geçtiğinizde istirahat modunu başlatın. Sefer dışı veya mola ihlali sürüşlerinde sistem sizi uyarır.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onToggleRestMode(true) },
                    enabled = !isTripActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_start_rest_mode"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFC084FC)
                    )
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTripActive) "Sefer Sırasında İstirahat Başlatılamaz" else "İstirahat Modunu Başlat (Mola)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
