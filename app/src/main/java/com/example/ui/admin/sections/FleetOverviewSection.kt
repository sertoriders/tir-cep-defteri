package com.example.ui.admin.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.Trip
import com.example.data.model.Vehicle
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDarkest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FleetOverviewSection(
    vehicles: List<Vehicle>,
    activeTrips: List<Trip>,
    settings: AppSettings,
    onVehicleClick: (Vehicle) -> Unit
) {
    var selectedVehicleId by remember { mutableStateOf(vehicles.firstOrNull()?.id ?: "") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Canlı Harita & Konum Takip Görünümü
        FleetMapViewer(
            vehicles = vehicles,
            selectedVehicleId = selectedVehicleId,
            onSelectVehicle = { selectedVehicleId = it }
        )

        // Kilometre Farkı Uyarıları (Threshold kontrolü)
        val discrepancyTrips = activeTrips.filter { it.kmDiscrepancy > settings.kmDiscrepancyThreshold }
        if (discrepancyTrips.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GÜZERGÂH SAYAC-GPS FARKI UYARISI (${discrepancyTrips.size} Araç)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                    discrepancyTrips.forEach { trip ->
                        Text(
                            text = "• ${trip.tripNumber}: Sayaç ile GPS arasında ${String.format(Locale.getDefault(), "%.1f", trip.kmDiscrepancy)} km fark var (Eşik: ${settings.kmDiscrepancyThreshold.toInt()} km).",
                            fontSize = 11.sp,
                            color = PureWhite,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Sefer Kayıtsız Sürüş / İstirahat Dışı Hareket Uyarısı
        val unrecordedAlertVehicles = vehicles.filter { it.unrecordedDriveAlert }
        if (unrecordedAlertVehicles.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF991B1B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = PureWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DİKKAT: SEFER KAYITSIZ SÜRÜŞ ALGILANDI (${unrecordedAlertVehicles.size} Araç)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                    unrecordedAlertVehicles.forEach { v ->
                        Text(
                            text = "• ${v.plate} (${v.name}): İstirahat/garaj modunda hareket algılandı! Şoföre bildirim gönderildi.",
                            fontSize = 11.sp,
                            color = PureWhite,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = "FİLO ARAÇLARI GENEL DURUMU (3 ARAÇ)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            letterSpacing = 1.sp
        )

        // 3 Aracın Detay Kartları
        vehicles.forEach { vehicle ->
            val trip = activeTrips.firstOrNull { it.vehicleId == vehicle.id }
            AdminVehicleCard(
                vehicle = vehicle,
                activeTrip = trip,
                isSelected = vehicle.id == selectedVehicleId,
                onClick = {
                    selectedVehicleId = vehicle.id
                    onVehicleClick(vehicle)
                }
            )
        }
    }
}

@Composable
fun FleetMapViewer(
    vehicles: List<Vehicle>,
    selectedVehicleId: String,
    onSelectVehicle: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDarkest)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Harita Çizim Canvası (Şematik Türkiye Haritası & GPS Koordinat Noktaları)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Şebeke / Radar Grid Çizimi
                val gridColor = Color(0xFF1E293B)
                val strokeWidth = 1f
                for (x in 0..10) {
                    val xPos = w * (x / 10f)
                    drawLine(gridColor, Offset(xPos, 0f), Offset(xPos, h), strokeWidth)
                }
                for (y in 0..6) {
                    val yPos = h * (y / 6f)
                    drawLine(gridColor, Offset(0f, yPos), Offset(w, yPos), strokeWidth)
                }

                // Şehir Rotaları (İstanbul, Ankara, İzmir ana koridorları)
                val ist = Offset(w * 0.22f, h * 0.30f)
                val ank = Offset(w * 0.52f, h * 0.45f)
                val izm = Offset(w * 0.18f, h * 0.70f)

                val routeColor = Color(0xFF334155)
                val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                drawLine(routeColor, ist, ank, strokeWidth = 3f, pathEffect = dashedEffect)
                drawLine(routeColor, ist, izm, strokeWidth = 3f, pathEffect = dashedEffect)
                drawLine(routeColor, izm, ank, strokeWidth = 3f, pathEffect = dashedEffect)

                // Araç Konumları
                vehicles.forEachIndexed { index, vehicle ->
                    val pos = when (index) {
                        0 -> ist // Ford Cargo 1838 (İstanbul)
                        1 -> ank // Ford F-MAX (Ankara)
                        else -> izm // Renault Premium 430 (İzmir)
                    }

                    val isSelected = vehicle.id == selectedVehicleId
                    val markerColor = when (vehicle.status) {
                        "LOADED" -> StatusSuccess
                        "EMPTY" -> BlueAccent
                        "MAINTENANCE" -> StatusWarning
                        else -> CyanAccent
                    }

                    // Canlı Sinyal Halkası
                    drawCircle(
                        color = markerColor.copy(alpha = if (isSelected) 0.35f else 0.15f),
                        radius = if (isSelected) 34f else 22f,
                        center = pos
                    )

                    // İç Nokta
                    drawCircle(
                        color = markerColor,
                        radius = if (isSelected) 12f else 8f,
                        center = pos
                    )

                    // Beyaz Çekirdek
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = pos
                    )
                }
            }

            // Üst Bilgi Barı
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CANLI UYDU & FİLO HARİTASI (15s Hedef)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
                Text(
                    text = "İstanbul, Ankara, İzmir koridorlarındaki araçlar",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Alt Araç Hızlı Seçicisi
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(NavySurface.copy(alpha = 0.9f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                vehicles.forEach { v ->
                    val isSelected = v.id == selectedVehicleId
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectVehicle(v.id) }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = v.plate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyanAccent else PureWhite
                        )
                        Text(
                            text = when {
                                v.status == "RESTING" || v.isResting -> "İstirahat"
                                v.status == "LOADED" -> "Yüklü"
                                v.status == "EMPTY" -> "Boş"
                                else -> "Garajda"
                            },
                            fontSize = 9.sp,
                            color = if (isSelected) CyanAccent else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminVehicleCard(
    vehicle: Vehicle,
    activeTrip: Trip?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val fuelPercent = (vehicle.currentFuelLiters / vehicle.tankCapacity).coerceIn(0.0, 1.0).toFloat()
    val isLoaded = vehicle.status == "LOADED"
    val consumption = if (isLoaded) vehicle.avgConsumptionLoaded else vehicle.avgConsumptionEmpty
    val estimatedRange = (vehicle.currentFuelLiters / (consumption / 100.0)).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(1.5.dp, CyanAccent, RoundedCornerShape(14.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavySurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = vehicle.plate,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = vehicle.name,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                val (badgeText, badgeColor) = when {
                    vehicle.status == "RESTING" || vehicle.isResting -> "İSTİRAHATTE" to Color(0xFF8B5CF6)
                    vehicle.status == "LOADED" -> "YÜKLÜ" to StatusSuccess
                    vehicle.status == "EMPTY" -> "BOŞ SEYİR" to BlueAccent
                    vehicle.status == "MAINTENANCE" -> "BAKIMDA" to StatusWarning
                    else -> "GARAJDA" to Color(0xFF64748B)
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            if (vehicle.unrecordedDriveAlert) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF7F1D1D), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚠️ SÜRÜŞ UYARISI: Sefer kaydı veya istirahat dışı hareket algılandı!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Şoför ve Son Konum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Atanan Şoför: ${vehicle.assignedDriverName.ifBlank { "Henüz Atanmadı" }}",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "Sayaç: ${String.format(Locale.getDefault(), "%,.0f km", vehicle.odometerKm)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val timeStr = if (vehicle.lastLocationTime != null) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    sdf.format(vehicle.lastLocationTime.toDate())
                } else "Yeni"
                Text(
                    text = "${vehicle.lastLocationAddress} (Son Güncelleme: $timeStr)",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Yakıt ve Menzil
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Depo: ${vehicle.currentFuelLiters.toInt()} / ${vehicle.tankCapacity.toInt()} L (%${(fuelPercent * 100).toInt()})",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "Tahmini Menzil: ~$estimatedRange km",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { fuelPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (fuelPercent < 0.2f) StatusDanger else CyanAccent,
                trackColor = NavySurface
            )

            if (activeTrip != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySurface, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "Aktif Sefer: ${activeTrip.tripNumber} (${activeTrip.origin} ➔ ${activeTrip.destination})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "GPS Gidilen: ${String.format(Locale.getDefault(), "%.1f km", activeTrip.gpsRouteKm)} | Navlun: ${String.format(Locale.getDefault(), "%,.0f TL", activeTrip.freightPrice)}",
                            fontSize = 10.sp,
                            color = CyanAccent
                        )
                    }
                }
            }
        }
    }
}
