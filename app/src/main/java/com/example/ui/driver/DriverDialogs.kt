package com.example.ui.driver

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
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
import java.util.Locale

@Composable
fun StartTripDialog(
    tripType: String, // "LOADED" veya "EMPTY"
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (
        origin: String,
        destination: String,
        startKm: Double,
        freightPrice: Double,
        paymentType: String,
        paymentDueDate: String
    ) -> Unit
) {
    var origin by remember { mutableStateOf(if (tripType == "LOADED") "İstanbul Gebze" else vehicle.lastLocationAddress) }
    var destination by remember { mutableStateOf(if (tripType == "LOADED") "Ankara Lojistik Üssü" else "Bursa Nilüfer") }
    var startKmText by remember { mutableStateOf(vehicle.odometerKm.toInt().toString()) }
    var freightPriceText by remember { mutableStateOf(if (tripType == "LOADED") "45000" else "0") }
    var paymentType by remember { mutableStateOf("VADELİ") }
    var paymentDueDate by remember { mutableStateOf("30 Gün Vadeli") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (tripType == "LOADED") "Yükü Aldım (Dolu Sefer)" else "Boş Seyir Başlat",
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Araç: ${vehicle.plate} (${vehicle.name})",
                    fontSize = 12.sp,
                    color = CyanAccent
                )

                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Yükleme / Çıkış Yeri") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Teslimat / Varış Yeri") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = startKmText,
                    onValueChange = { startKmText = it },
                    label = { Text("Başlangıç Kilometresi (Sayac)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                if (tripType == "LOADED") {
                    OutlinedTextField(
                        value = freightPriceText,
                        onValueChange = { freightPriceText = it },
                        label = { Text("Nakliye Bedeli (TL - İsteğe Bağlı)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors()
                    )

                    OutlinedTextField(
                        value = paymentType,
                        onValueChange = { paymentType = it },
                        label = { Text("Ödeme Şekli (Nakit / Havale / Vadeli)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors()
                    )

                    OutlinedTextField(
                        value = paymentDueDate,
                        onValueChange = { paymentDueDate = it },
                        label = { Text("Ödeme Vadesi (İsteğe Bağlı)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors()
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Başlangıç konumu otomatik kaydedilecek ve 15 saniyede bir GPS takibi başlatılacaktır.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val km = startKmText.toDoubleOrNull() ?: vehicle.odometerKm
                    val price = freightPriceText.toDoubleOrNull() ?: 0.0
                    onConfirm(origin, destination, km, price, paymentType, paymentDueDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
            ) {
                Text("Seferi Başlat", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal", color = PureWhite)
            }
        },
        containerColor = NavyCard
    )
}

@Composable
fun EndTripDialog(
    trip: Trip,
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (endKm: Double) -> Unit
) {
    var endKmText by remember {
        mutableStateOf((trip.startKm + trip.gpsRouteKm.coerceAtLeast(50.0)).toInt().toString())
    }

    val endKm = endKmText.toDoubleOrNull() ?: trip.startKm
    val odometerDistance = (endKm - trip.startKm).coerceAtLeast(0.0)
    val discrepancy = Math.abs(odometerDistance - trip.gpsRouteKm)
    val hasDiscrepancyWarning = discrepancy > 10.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seferi Bitir (${trip.tripNumber})", fontWeight = FontWeight.Bold, color = PureWhite)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Güzergâh: ${trip.origin} ➔ ${trip.destination}",
                    fontSize = 13.sp,
                    color = PureWhite,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Başlangıç Km: ${trip.startKm.toInt()} km",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "GPS ile Ölçülen Mesafe: ${String.format(Locale.getDefault(), "%.1f km", trip.gpsRouteKm)}",
                    fontSize = 12.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = endKmText,
                    onValueChange = { endKmText = it },
                    label = { Text("Bitiş Kilometresi (Sayaç)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                Text(
                    text = "Sayaç Farkı: ${odometerDistance.toInt()} km",
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0)
                )

                if (hasDiscrepancyWarning) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DİKKAT: Sayaç ile GPS arasında ${String.format(Locale.getDefault(), "%.1f", discrepancy)} km fark var! Bu durum yöneticiye otomatik uyarı olarak bildirilecektir.",
                                fontSize = 11.sp,
                                color = PureWhite
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(endKm) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess, contentColor = PureWhite)
            ) {
                Text("Teslimatı Onayla ve Bitir", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal", color = PureWhite)
            }
        },
        containerColor = NavyCard
    )
}

@Composable
fun FuelEntryDialog(
    vehicle: Vehicle,
    activeTrip: Trip?,
    onDismiss: () -> Unit,
    onConfirm: (liters: Double, pricePerLiter: Double, receiptUrl: String) -> Unit
) {
    var litersText by remember { mutableStateOf("250") }
    var pricePerLiterText by remember { mutableStateOf("44.50") }
    var receiptPhotoAdded by remember { mutableStateOf(false) }

    val liters = litersText.toDoubleOrNull() ?: 0.0
    val price = pricePerLiterText.toDoubleOrNull() ?: 0.0
    val totalAmount = liters * price

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Yakıt Girişi", fontWeight = FontWeight.Bold, color = PureWhite)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Araç: ${vehicle.plate} | Mevcut Depo: ${vehicle.currentFuelLiters.toInt()} / ${vehicle.tankCapacity.toInt()} L",
                    fontSize = 12.sp,
                    color = CyanAccent
                )

                if (activeTrip != null) {
                    Text(
                        text = "Aktif Sefer: ${activeTrip.tripNumber} (${activeTrip.origin} ➔ ${activeTrip.destination})",
                        fontSize = 11.sp,
                        color = StatusSuccess
                    )
                }

                OutlinedTextField(
                    value = litersText,
                    onValueChange = { litersText = it },
                    label = { Text("Alınan Litre") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = pricePerLiterText,
                    onValueChange = { pricePerLiterText = it },
                    label = { Text("Litre Fiyatı (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                // Otomatik Toplam Tutar
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Hesaplanan Toplam Tutar",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,.2f", totalAmount)} TL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                }

                // Fiş Fotoğrafı Ekleme
                OutlinedButton(
                    onClick = { receiptPhotoAdded = !receiptPhotoAdded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (receiptPhotoAdded) StatusSuccess else PureWhite
                    )
                ) {
                    Icon(
                        imageVector = if (receiptPhotoAdded) Icons.Default.Check else Icons.Default.CameraAlt,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (receiptPhotoAdded) "Yakıt Fişi Eklendi (FIS-0912)" else "Yakıt Fişi Fotoğrafı Ekle")
                }

                Text(
                    text = "Konum ve tarih bilgisi telefon GPS'i üzerinden otomatik kaydedilecektir.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (liters > 0 && price > 0) {
                        onConfirm(liters, price, if (receiptPhotoAdded) "receipt_photo_attached" else "")
                    }
                },
                enabled = liters > 0 && price > 0,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
            ) {
                Text("Yakıtı Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal", color = PureWhite)
            }
        },
        containerColor = NavyCard
    )
}

@Composable
fun ExpenseEntryDialog(
    vehicle: Vehicle,
    activeTrip: Trip?,
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Double, description: String, receiptUrl: String) -> Unit
) {
    val categories = listOf("YEMEK", "KAHVE", "SIGARA", "PARK", "TAMIR", "LASTIK", "HGS", "YIKAMA", "DIGER")
    val categoryLabels = mapOf(
        "YEMEK" to "Yemek",
        "KAHVE" to "Kahve / İçecek",
        "SIGARA" to "Sigara",
        "PARK" to "Otopark",
        "TAMIR" to "Tamir / Bakım",
        "LASTIK" to "Lastik",
        "HGS" to "HGS / Otoyol",
        "YIKAMA" to "Yıkama",
        "DIGER" to "Diğer Masraf"
    )

    var selectedCategory by remember { mutableStateOf("YEMEK") }
    var amountText by remember { mutableStateOf("350") }
    var description by remember { mutableStateOf("") }
    var receiptPhotoAdded by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = CyanAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Masraf Girişi", fontWeight = FontWeight.Bold, color = PureWhite)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Masraf Kategorisi Seçin:", fontSize = 12.sp, color = PureWhite)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) CyanAccent else NavySurface,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = categoryLabels[cat] ?: cat,
                                color = if (isSelected) NavyDarkest else PureWhite,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Masraf Tutarı (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama (Örn: Bolu Dağı Yemek)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                // Fiş Fotoğrafı Ekleme
                OutlinedButton(
                    onClick = { receiptPhotoAdded = !receiptPhotoAdded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (receiptPhotoAdded) StatusSuccess else PureWhite
                    )
                ) {
                    Icon(
                        imageVector = if (receiptPhotoAdded) Icons.Default.Check else Icons.Default.CameraAlt,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (receiptPhotoAdded) "Harcama Fişi Eklendi" else "Harcama Fişi Fotoğrafı Ekle")
                }

                Text(
                    text = "Şoförler onaylanmış masraf kayıtlarını doğrudan silemez veya değiştiremez. Hata durumunda düzeltme talebi gönderebilirsiniz.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount > 0) {
                        onConfirm(selectedCategory, amount, description, if (receiptPhotoAdded) "receipt_photo" else "")
                    }
                },
                enabled = amount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
            ) {
                Text("Masrafı Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal", color = PureWhite)
            }
        },
        containerColor = NavyCard
    )
}

@Composable
fun CorrectionRequestDialog(
    vehicleId: String,
    onDismiss: () -> Unit,
    onConfirm: (targetType: String, targetId: String, details: String) -> Unit
) {
    val types = listOf("EXPENSE", "FUEL", "TRIP")
    val typeLabels = mapOf("EXPENSE" to "Masraf", "FUEL" to "Yakıt", "TRIP" to "Sefer")
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var targetId by remember { mutableStateOf("") }
    var requestedChanges by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Düzeltme Talebi Gönder", fontWeight = FontWeight.Bold, color = PureWhite)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Düzeltilmesini istediğiniz kayıt türünü ve doğru bilgileri açıklayınız. Yönetici inceleyip onaylayacaktır.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { t ->
                        val isSelected = selectedType == t
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) CyanAccent else NavySurface,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = t }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = typeLabels[t] ?: t,
                                color = if (isSelected) NavyDarkest else PureWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetId,
                    onValueChange = { targetId = it },
                    label = { Text("Kayıt Tarihi veya Fiş No") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = requestedChanges,
                    onValueChange = { requestedChanges = it },
                    label = { Text("Talep Edilen Düzeltme (Hatalı tutar / km vb.)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = dialogTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (requestedChanges.isNotBlank()) {
                        onConfirm(selectedType, targetId.ifBlank { "KAYIT" }, requestedChanges)
                    }
                },
                enabled = requestedChanges.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
            ) {
                Text("Talebi İlet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal", color = PureWhite)
            }
        },
        containerColor = NavyCard
    )
}

@Composable
fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PureWhite,
    unfocusedTextColor = PureWhite,
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = Color(0xFF475569),
    focusedLabelColor = CyanAccent,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedContainerColor = NavySurface,
    unfocusedContainerColor = NavySurface
)
