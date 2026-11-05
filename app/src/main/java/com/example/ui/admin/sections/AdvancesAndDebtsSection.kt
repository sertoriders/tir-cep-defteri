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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Advance
import com.example.data.model.DebtRecord
import com.example.data.model.ExpenseEntry
import com.example.data.model.UserProfile
import com.example.ui.driver.dialogTextFieldColors
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
fun AdvancesAndDebtsSection(
    advances: List<Advance>,
    expenses: List<ExpenseEntry>,
    debts: List<DebtRecord>,
    drivers: List<UserProfile>,
    onAddAdvance: (driverId: String, driverName: String, amount: Double, desc: String, date: String) -> Unit,
    onAddDebtRecord: (type: String, company: String, amount: Double, dueDate: String, freq: String, notes: String) -> Unit,
    onRecordDebtPayment: (debtId: String, amount: Double) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Şoför Avansları", "Alacak & Verecek Takibi")

    var showAddAdvanceDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<DebtRecord?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavyCard,
            contentColor = CyanAccent
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) CyanAccent else Color(0xFF94A3B8)
                        )
                    }
                )
            }
        }

        if (selectedTab == 0) {
            // Şoför Avansları Görünümü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ŞOFÖR BAZINDA AVANS VE HARCAMA MUTABAKATI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Button(
                    onClick = { showAddAdvanceDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Avans Gönder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Her şoför için avans - masraf dengesi
            val driverList = if (drivers.isNotEmpty()) drivers else listOf(
                UserProfile(userId = "driver_1", name = "Ahmet Yılmaz (Cargo 1838)"),
                UserProfile(userId = "driver_2", name = "Mehmet Kaya (F-MAX)")
            )

            driverList.forEach { driver ->
                val dAdvances = advances.filter { it.driverId == driver.userId }
                val dExpenses = expenses.filter { it.driverId == driver.userId }
                val totalAdv = dAdvances.sumOf { it.amount }
                val totalExp = dExpenses.sumOf { it.amount }
                val balance = totalAdv - totalExp // Şoförün elinde kalan avans

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
                            Text(
                                text = driver.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            val balanceColor = if (balance >= 0) StatusSuccess else StatusDanger
                            Text(
                                text = "Kalan Bakiye: ${String.format(Locale.getDefault(), "%,.0f TL", balance)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = balanceColor
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gönderilen Toplam Avans: ${String.format(Locale.getDefault(), "%,.0f TL", totalAdv)}",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "Şoförün Kaydettiği Masraf: ${String.format(Locale.getDefault(), "%,.0f TL", totalExp)}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        if (dAdvances.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Son Avans Kayıtları:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            dAdvances.take(3).forEach { adv ->
                                Text(
                                    text = "• ${adv.date}: ${String.format(Locale.getDefault(), "%,.0f TL", adv.amount)} (${adv.description})",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Alacak & Verecek Takibi Görünümü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VADELİ ALACAK VE VERECEKLER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Button(
                    onClick = { showAddDebtDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yeni Kayıt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            val totalReceivable = debts.filter { it.type == "RECEIVABLE" && it.status != "PAID" }.sumOf { it.remainingAmount }
            val totalPayable = debts.filter { it.type == "PAYABLE" && it.status != "PAID" }.sumOf { it.remainingAmount }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Toplam Alacak", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,.0f TL", totalReceivable)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Toplam Verecek", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,.0f TL", totalPayable)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusDanger
                        )
                    }
                }
            }

            if (debts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)
                ) {
                    Text(
                        text = "Henüz alacak veya verecek kaydı eklenmedi.",
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp
                    )
                }
            }

            debts.forEach { debt ->
                val isReceivable = debt.type == "RECEIVABLE"
                val isOverdue = debt.status == "OVERDUE" || (debt.status != "PAID" && debt.dueDate.contains("Geçti", ignoreCase = true))

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isReceivable) StatusSuccess.copy(alpha = 0.2f) else StatusDanger.copy(alpha = 0.2f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isReceivable) "ALACAK" else "VERECEK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReceivable) StatusSuccess else StatusDanger
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = debt.companyOrPerson,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }

                            if (isOverdue) {
                                Box(
                                    modifier = Modifier
                                        .background(StatusDanger, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "VADESİ GEÇTİ!", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                                }
                            } else {
                                Text(
                                    text = when (debt.status) {
                                        "PAID" -> "ÖDENDİ"
                                        "PARTIAL" -> "KISMİ ÖDENDİ"
                                        else -> "BEKLİYOR"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (debt.status == "PAID") StatusSuccess else Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Toplam: ${String.format(Locale.getDefault(), "%,.0f TL", debt.totalAmount)}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "Kalan: ${String.format(Locale.getDefault(), "%,.0f TL", debt.remainingAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isReceivable) StatusSuccess else StatusDanger
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vade Tarihi: ${debt.dueDate} (${debt.paymentFrequency})",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )

                            if (debt.status != "PAID") {
                                OutlinedButton(
                                    onClick = { selectedDebtForPayment = debt },
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                                ) {
                                    Text("Ödeme Gir", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Avans Ekleme Dialogu
    if (showAddAdvanceDialog) {
        AddAdvanceDialog(
            drivers = drivers,
            onDismiss = { showAddAdvanceDialog = false },
            onConfirm = { driverId, driverName, amount, desc, date ->
                showAddAdvanceDialog = false
                onAddAdvance(driverId, driverName, amount, desc, date)
            }
        )
    }

    // Alacak / Verecek Ekleme Dialogu
    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onConfirm = { type, company, amount, dueDate, freq, notes ->
                showAddDebtDialog = false
                onAddDebtRecord(type, company, amount, dueDate, freq, notes)
            }
        )
    }

    // Kısmi / Tam Ödeme Girişi Dialogu
    if (selectedDebtForPayment != null) {
        val debt = selectedDebtForPayment!!
        RecordPaymentDialog(
            debt = debt,
            onDismiss = { selectedDebtForPayment = null },
            onConfirm = { paymentAmount ->
                selectedDebtForPayment = null
                onRecordDebtPayment(debt.id, paymentAmount)
            }
        )
    }
}

@Composable
fun AddAdvanceDialog(
    drivers: List<UserProfile>,
    onDismiss: () -> Unit,
    onConfirm: (driverId: String, driverName: String, amount: Double, desc: String, date: String) -> Unit
) {
    var selectedDriverId by remember { mutableStateOf(drivers.firstOrNull()?.userId ?: "driver_1") }
    var selectedDriverName by remember { mutableStateOf(drivers.firstOrNull()?.name ?: "Şoför") }
    var amountText by remember { mutableStateOf("5000") }
    var description by remember { mutableStateOf("Haftalık Sefer Avansı") }
    var dateText by remember { mutableStateOf(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Şoföre Avans Gönder", fontWeight = FontWeight.Bold, color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = selectedDriverName,
                    onValueChange = { selectedDriverName = it },
                    label = { Text("Şoför Adı") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Avans Tutarı (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama (Banka/Elden)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Tarih") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(selectedDriverId, selectedDriverName, amount, description, dateText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
            ) {
                Text("Kaydet", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("İptal", color = PureWhite) }
        },
        containerColor = NavyCard
    )
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, company: String, amount: Double, dueDate: String, freq: String, notes: String) -> Unit
) {
    var type by remember { mutableStateOf("RECEIVABLE") } // RECEIVABLE veya PAYABLE
    var company by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("60000") }
    var dueDate by remember { mutableStateOf("15.12.2026") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var notes by remember { mutableStateOf("Nakliye navlun bakiyesi") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Alacak / Verecek Kaydı", fontWeight = FontWeight.Bold, color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { type = "RECEIVABLE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "RECEIVABLE") StatusSuccess else NavySurface
                        )
                    ) { Text("Alacak (Firma)") }
                    Button(
                        onClick = { type = "PAYABLE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "PAYABLE") StatusDanger else NavySurface
                        )
                    ) { Text("Verecek (Borç)") }
                }

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Firma veya Kişi Adı") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Tutar (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Vade Tarihi") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Açıklama / Notlar") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (company.isNotBlank() && amount > 0) {
                        onConfirm(type, company, amount, dueDate, frequency, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest)
            ) {
                Text("Ekle", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("İptal", color = PureWhite) }
        },
        containerColor = NavyCard
    )
}

@Composable
fun RecordPaymentDialog(
    debt: DebtRecord,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var paymentAmountText by remember { mutableStateOf(debt.remainingAmount.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ödeme Tahsil / İtfa Kaydı", fontWeight = FontWeight.Bold, color = PureWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Muhatap: ${debt.companyOrPerson}", fontSize = 13.sp, color = PureWhite)
                Text(
                    text = "Kalan Bakiye: ${String.format(Locale.getDefault(), "%,.0f TL", debt.remainingAmount)}",
                    fontSize = 12.sp,
                    color = CyanAccent
                )
                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Yapılan / Alınan Ödeme (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = paymentAmountText.toDoubleOrNull() ?: 0.0
                    if (p > 0) onConfirm(p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess, contentColor = PureWhite)
            ) {
                Text("Ödemeyi İşle", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("İptal", color = PureWhite) }
        },
        containerColor = NavyCard
    )
}
