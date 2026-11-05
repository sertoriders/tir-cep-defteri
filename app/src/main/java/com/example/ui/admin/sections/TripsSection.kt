package com.example.ui.admin.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Locale

@Composable
fun TripsSection(
    trips: List<Trip>,
    vehicles: List<Vehicle>,
    kmThreshold: Double,
    onTogglePaymentStatus: (Trip) -> Unit
) {
    val filters = listOf("TÜMÜ", "AKTİF", "TAMAMLANAN", "DOLU", "BOŞ", "UYARILI")
    var selectedFilter by remember { mutableStateOf("TÜMÜ") }

    val filteredTrips = trips.filter { trip ->
        when (selectedFilter) {
            "AKTİF" -> trip.status == "ACTIVE"
            "TAMAMLANAN" -> trip.status == "COMPLETED"
            "DOLU" -> trip.tripType == "LOADED"
            "BOŞ" -> trip.tripType == "EMPTY"
            "UYARILI" -> trip.kmDiscrepancy > kmThreshold
            else -> true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Filtre Çubuğu
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f ->
                val isSelected = selectedFilter == f
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) CyanAccent else NavyCard,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedFilter = f }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = f,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NavyDarkest else PureWhite
                    )
                }
            }
        }

        Text(
            text = "SEFER GEÇMİŞİ VE GPS KİLOMETRE RAPORU (${filteredTrips.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            letterSpacing = 1.sp
        )

        if (filteredTrips.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Text(
                    text = "Kayıtlı sefer bulunmuyor.",
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp
                )
            }
        }

        filteredTrips.forEach { trip ->
            val vehicle = vehicles.firstOrNull { it.id == trip.vehicleId }
            val plate = vehicle?.plate ?: "TIR"
            val hasDiscrepancyWarning = trip.kmDiscrepancy > kmThreshold

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = trip.tripNumber,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = "$plate • ${trip.driverName}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        val (statusText, statusColor) = if (trip.status == "ACTIVE") {
                            "SEFERDE" to StatusSuccess
                        } else {
                            "TAMAMLANDI" to Color(0xFF64748B)
                        }

                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Güzergâh
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${trip.origin} ➔ ${trip.destination}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Kilometre Karşılaştırması
                    val odometerDiff = (trip.endKm - trip.startKm).coerceAtLeast(0.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sayaç: ${trip.startKm.toInt()} ➔ ${trip.endKm.toInt()} km (${odometerDiff.toInt()} km)",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "GPS: ${String.format(Locale.getDefault(), "%.1f km", trip.gpsRouteKm)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }

                    // Kilometre Fark Uyarısı (Eşik aşılmışsa)
                    if (hasDiscrepancyWarning) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(6.dp),
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
                                text = "Fark: ${String.format(Locale.getDefault(), "%.1f", trip.kmDiscrepancy)} km (Limit ${kmThreshold.toInt()} km aşıldı!)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }

                    // Navlun & Tahsilat
                    if (trip.tripType == "LOADED" && trip.freightPrice > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NavySurface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Navlun Bedeli: ${String.format(Locale.getDefault(), "%,.0f TL", trip.freightPrice)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Ödeme: ${trip.paymentType} (${trip.paymentDueDate.ifBlank { "Vade Yok" }})",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Row(
                                modifier = Modifier.clickable { onTogglePaymentStatus(trip) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (trip.isPaid) Icons.Default.CheckCircle else Icons.Default.Pending,
                                    contentDescription = null,
                                    tint = if (trip.isPaid) StatusSuccess else StatusWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (trip.isPaid) "Tahsil Edildi" else "Tahsil Bekliyor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (trip.isPaid) StatusSuccess else StatusWarning
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
