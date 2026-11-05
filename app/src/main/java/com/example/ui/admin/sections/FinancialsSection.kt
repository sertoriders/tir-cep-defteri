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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntry
import com.example.data.model.FuelEntry
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
fun FinancialsSection(
    trips: List<Trip>,
    fuels: List<FuelEntry>,
    expenses: List<ExpenseEntry>,
    vehicles: List<Vehicle>
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Kâr - Zarar Analizi", "Yakıt Raporu", "Masraf Dağılımı")

    // Gelir Hesaplamaları
    val accruedRevenue = trips.sumOf { it.freightPrice }
    val collectedCash = trips.filter { it.isPaid }.sumOf { it.freightPrice }
    val pendingReceivable = accruedRevenue - collectedCash

    // Gider Hesaplamaları
    val totalFuelCost = fuels.sumOf { it.totalAmount }
    val totalFuelLiters = fuels.sumOf { it.liters }
    val totalExpenseCost = expenses.sumOf { it.amount }
    val totalRecordedCosts = totalFuelCost + totalExpenseCost

    // Brüt Faaliyet Kârı
    val grossProfit = accruedRevenue - totalRecordedCosts

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

        when (selectedTab) {
            0 -> ProfitLossView(
                accruedRevenue = accruedRevenue,
                collectedCash = collectedCash,
                pendingReceivable = pendingReceivable,
                fuelCost = totalFuelCost,
                expenseCost = totalExpenseCost,
                totalRecordedCosts = totalRecordedCosts,
                grossProfit = grossProfit,
                vehicles = vehicles,
                trips = trips,
                fuels = fuels,
                expenses = expenses
            )
            1 -> FuelReportView(fuels = fuels, vehicles = vehicles)
            2 -> ExpenseReportView(expenses = expenses, vehicles = vehicles)
        }
    }
}

@Composable
fun ProfitLossView(
    accruedRevenue: Double,
    collectedCash: Double,
    pendingReceivable: Double,
    fuelCost: Double,
    expenseCost: Double,
    totalRecordedCosts: Double,
    grossProfit: Double,
    vehicles: List<Vehicle>,
    trips: List<Trip>,
    fuels: List<FuelEntry>,
    expenses: List<ExpenseEntry>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Önemli Uyarı Kartı (Kullanıcı İsteği: "Girilmeyen giderleri sıfır varsayıp kesin net kâr gibi gösterme")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DİKKAT: Aşağıdaki kâr hesabı sisteme fiilen girilen yakıt ve sefer masraflarına dayalı 'Brüt Faaliyet Marjı'dır. Amortisman, kasko, sigorta, MTV ve sisteme henüz girilmemiş tamir giderleri sıfır kabul edilerek kesin kâr yanılsaması oluşturulmaz.",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }

        // Ana Metrik Kartları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Tahakkuk Eden Gelir",
                amount = accruedRevenue,
                subtitle = "Faturalandırılan Toplam Navlun",
                color = PureWhite,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Tahsil Edilen Nakit",
                amount = collectedCash,
                subtitle = "Kasaya Giren Para",
                color = StatusSuccess,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Toplam Kayıtlı Gider",
                amount = totalRecordedCosts,
                subtitle = "Yakıt (${fuelCost.toInt()} TL) + Masraflar",
                color = StatusDanger,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Faaliyet Kârı (Brüt)",
                amount = grossProfit,
                subtitle = if (grossProfit >= 0) "Pozitif Faaliyet Marjı" else "Zarar",
                color = if (grossProfit >= 0) StatusSuccess else StatusDanger,
                modifier = Modifier.weight(1f)
            )
        }

        // Araç Başına Gelir ve Gider Dağılımı
        Text(
            text = "ARAÇ BAZINDA GELİR VE GİDER PERFORMANSI",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        vehicles.forEach { vehicle ->
            val vTrips = trips.filter { it.vehicleId == vehicle.id }
            val vFuels = fuels.filter { it.vehicleId == vehicle.id }
            val vExpenses = expenses.filter { it.vehicleId == vehicle.id }

            val vRevenue = vTrips.sumOf { it.freightPrice }
            val vFuelCost = vFuels.sumOf { it.totalAmount }
            val vExpenseCost = vExpenses.sumOf { it.amount }
            val vTotalCost = vFuelCost + vExpenseCost
            val vProfit = vRevenue - vTotalCost

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = vehicle.plate,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "Kâr/Zarar: ${String.format(Locale.getDefault(), "%,.0f TL", vProfit)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vProfit >= 0) StatusSuccess else StatusDanger
                        )
                    }

                    Text(
                        text = vehicle.name,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gelir: ${String.format(Locale.getDefault(), "%,.0f TL", vRevenue)}",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "Yakıt: ${String.format(Locale.getDefault(), "%,.0f TL", vFuelCost)}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Masraf: ${String.format(Locale.getDefault(), "%,.0f TL", vExpenseCost)}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FuelReportView(fuels: List<FuelEntry>, vehicles: List<Vehicle>) {
    val totalLiters = fuels.sumOf { it.liters }
    val totalCost = fuels.sumOf { it.totalAmount }
    val avgPricePerLiter = if (totalLiters > 0) totalCost / totalLiters else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "Toplam Yakıt Tüketimi", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text(
                    text = "${String.format(Locale.getDefault(), "%,.1f Litre", totalLiters)} (Toplam: ${String.format(Locale.getDefault(), "%,.0f TL", totalCost)})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Text(
                    text = "Ağırlıklı Ortalama Litre Fiyatı: ${String.format(Locale.getDefault(), "%.2f TL", avgPricePerLiter)}",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        Text(
            text = "ARAÇ TÜKETİM DEĞERLERİ (YÜKLÜ VS BOŞ)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            letterSpacing = 1.sp
        )

        vehicles.forEach { v ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "${v.plate} - ${v.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Yüklü Tüketim: ${v.avgConsumptionLoaded} L / 100km",
                            fontSize = 12.sp,
                            color = StatusWarning
                        )
                        Text(
                            text = "Boş Tüketim: ${v.avgConsumptionEmpty} L / 100km",
                            fontSize = 12.sp,
                            color = BlueAccent
                        )
                    }
                    Text(
                        text = "Depo Kapasitesi: ${v.tankCapacity.toInt()} Litre | Mevcut: ${v.currentFuelLiters.toInt()} L",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseReportView(expenses: List<ExpenseEntry>, vehicles: List<Vehicle>) {
    val categoryTotals = expenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val totalExpense = expenses.sumOf { it.amount }

    val categoryLabels = mapOf(
        "YEMEK" to "Yemek Giderleri",
        "KAHVE" to "Kahve / İçecek",
        "SIGARA" to "Sigara",
        "PARK" to "Otopark & Garaj",
        "TAMIR" to "Tamir & Bakım",
        "LASTIK" to "Lastik Değişimi & Onarım",
        "HGS" to "HGS & Otoyol Ücretleri",
        "YIKAMA" to "Araç Yıkama & Temizlik",
        "DIGER" to "Diğer Masraflar"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "Toplam Saha & Sefer Masrafları", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text(
                    text = "${String.format(Locale.getDefault(), "%,.0f TL", totalExpense)} (${expenses.size} Fiş)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusWarning
                )
            }
        }

        Text(
            text = "KATEGORİ BAZINDA DAĞILIM",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            letterSpacing = 1.sp
        )

        categoryLabels.forEach { (catKey, label) ->
            val catTotal = categoryTotals[catKey] ?: 0.0
            val ratio = if (totalExpense > 0) (catTotal / totalExpense).toFloat() else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,.0f TL", catTotal)} (%${(ratio * 100).toInt()})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanAccent,
                        trackColor = NavySurface
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: Double,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format(Locale.getDefault(), "%,.0f TL", amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFFCBD5E1))
        }
    }
}
