package com.example.ui.admin.sections

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.AppSettings
import com.example.data.model.CorrectionRequest
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.ui.driver.dialogTextFieldColors
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDarkest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun ManagementSection(
    vehicles: List<Vehicle>,
    correctionRequests: List<CorrectionRequest>,
    settings: AppSettings,
    onAssignDriver: (vehicleId: String, driverId: String, driverName: String) -> Unit,
    onResolveCorrection: (reqId: String, approved: Boolean, note: String) -> Unit,
    onUpdateSettings: (threshold: Double, intervalSeconds: Long) -> Unit
) {
    var kmThresholdText by remember { mutableStateOf(settings.kmDiscrepancyThreshold.toInt().toString()) }
    var intervalSecondsText by remember { mutableStateOf(settings.gpsUpdateIntervalSeconds.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Sistem ve GPS Fark Uyarı Ayarları
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SİSTEM VE GPS UYARI AYARLARI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = kmThresholdText,
                    onValueChange = { kmThresholdText = it },
                    label = { Text("Sayaç - GPS Farkı Uyarı Eşiği (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = intervalSecondsText,
                    onValueChange = { intervalSecondsText = it },
                    label = { Text("Hedef GPS Güncelleme Aralığı (Saniye)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val threshold = kmThresholdText.toDoubleOrNull() ?: 10.0
                        val interval = intervalSecondsText.toLongOrNull() ?: 15L
                        onUpdateSettings(threshold, interval)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
                ) {
                    Text("Ayarları Güncelle", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Araç & Şoför Atamaları
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ARAÇ ŞOFÖR ATAMALARI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                vehicles.forEach { vehicle ->
                    var driverNameInput by remember { mutableStateOf(vehicle.assignedDriverName) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = vehicle.plate, fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 13.sp)
                            Text(text = vehicle.name, fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }

                        OutlinedTextField(
                            value = driverNameInput,
                            onValueChange = { driverNameInput = it },
                            placeholder = { Text("Şoför Adı", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .padding(horizontal = 4.dp),
                            colors = dialogTextFieldColors()
                        )

                        Button(
                            onClick = { onAssignDriver(vehicle.id, "driver_${vehicle.id}", driverNameInput) },
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
                        ) {
                            Text("Ata", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Şoför Kayıt Düzeltme Talepleri
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ŞOFÖR KAYIT DÜZELTME TALEPLERİ (${correctionRequests.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (correctionRequests.isEmpty()) {
                    Text(
                        text = "Bekleyen düzeltme talebi bulunmuyor.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                correctionRequests.forEach { req ->
                    val isPending = req.status == "PENDING"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(NavySurface, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${req.driverName} • ${req.targetType}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PureWhite
                                )
                                Text(
                                    text = req.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (req.status) {
                                        "APPROVED" -> StatusSuccess
                                        "REJECTED" -> StatusDanger
                                        else -> StatusWarning
                                    }
                                )
                            }
                            Text(
                                text = "Talep: ${req.requestedChanges}",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            if (isPending) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { onResolveCorrection(req.id, false, "Yönetici tarafından reddedildi.") },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reddet", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onResolveCorrection(req.id, true, "Yönetici tarafından onaylandı ve düzeltildi.") },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess, contentColor = PureWhite)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Onayla", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
