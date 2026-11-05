package com.example.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.UserProfile
import com.example.data.repository.FleetRepository
import com.example.ui.admin.sections.AdvancesAndDebtsSection
import com.example.ui.admin.sections.FinancialsSection
import com.example.ui.admin.sections.FleetOverviewSection
import com.example.ui.admin.sections.ManagementSection
import com.example.ui.admin.sections.TripsSection
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDarkest
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    userProfile: UserProfile,
    repository: FleetRepository,
    onSignOut: () -> Unit,
    onSwitchToDriverMode: () -> Unit // Admin aynı zamanda kendi aracının işlemlerini yapabilsin
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val vehicles by repository.observeVehicles().collectAsState(initial = emptyList())
    val trips by repository.observeTrips().collectAsState(initial = emptyList())
    val fuels by repository.observeFuelEntries().collectAsState(initial = emptyList())
    val expenses by repository.observeExpenseEntries().collectAsState(initial = emptyList())
    val advances by repository.observeAdvances().collectAsState(initial = emptyList())
    val debts by repository.observeDebtRecords().collectAsState(initial = emptyList())
    val correctionRequests by repository.observeCorrectionRequests().collectAsState(initial = emptyList())
    val settings by repository.observeSettings().collectAsState(initial = AppSettings())
    val allUsers by repository.observeAllUsers().collectAsState(initial = emptyList())

    val adminTabs = listOf(
        "Filo & Harita",
        "Sefer Geçmişi",
        "Kâr & Giderler",
        "Avans & Alacaklar",
        "Yönetim & Ayarlar"
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TIR CEP DEFTERİ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "Yönetici Paneli • ${userProfile.name}",
                            fontSize = 11.sp,
                            color = CyanAccent
                        )
                    }
                },
                actions = {
                    // Admin kendi aracının işlemlerini yapabilsin
                    Button(
                        onClick = onSwitchToDriverMode,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = NavyDarkest),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Şoför Modu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Çıkış Yap", tint = PureWhite)
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
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = NavyDarkest,
                contentColor = CyanAccent,
                edgePadding = 12.dp
            ) {
                adminTabs.forEachIndexed { index, title ->
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> FleetOverviewSection(
                        vehicles = vehicles,
                        activeTrips = trips.filter { it.status == "ACTIVE" },
                        settings = settings,
                        onVehicleClick = { _ -> }
                    )
                    1 -> TripsSection(
                        trips = trips,
                        vehicles = vehicles,
                        kmThreshold = settings.kmDiscrepancyThreshold,
                        onTogglePaymentStatus = { trip ->
                            coroutineScope.launch {
                                try {
                                    repository.toggleTripPayment(trip.id, trip.isPaid)
                                    snackbarHostState.showSnackbar("Sefer ödeme durumu güncellendi.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                                }
                            }
                        }
                    )
                    2 -> FinancialsSection(
                        trips = trips,
                        fuels = fuels,
                        expenses = expenses,
                        vehicles = vehicles
                    )
                    3 -> AdvancesAndDebtsSection(
                        advances = advances,
                        expenses = expenses,
                        debts = debts,
                        drivers = allUsers.filter { it.role == "DRIVER" },
                        onAddAdvance = { driverId, driverName, amount, desc, date ->
                            coroutineScope.launch {
                                try {
                                    repository.addAdvance(driverId, driverName, amount, desc, date)
                                    snackbarHostState.showSnackbar("Avans kaydı eklendi.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                                }
                            }
                        },
                        onAddDebtRecord = { type, company, amount, dueDate, freq, notes ->
                            coroutineScope.launch {
                                try {
                                    repository.addDebtRecord(type, company, amount, dueDate, freq, notes)
                                    snackbarHostState.showSnackbar("Alacak/Verecek kaydı eklendi.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                                }
                            }
                        },
                        onRecordDebtPayment = { debtId, amount ->
                            coroutineScope.launch {
                                try {
                                    repository.recordDebtPayment(debtId, amount)
                                    snackbarHostState.showSnackbar("Ödeme başarıyla işlendi.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                                }
                            }
                        }
                    )
                    4 -> ManagementSection(
                        vehicles = vehicles,
                        correctionRequests = correctionRequests,
                        settings = settings,
                        onAssignDriver = { vehicleId, driverId, driverName ->
                            coroutineScope.launch {
                                try {
                                    repository.assignDriverToVehicle(vehicleId, driverId, driverName)
                                    snackbarHostState.showSnackbar("Şoför ataması güncellendi.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                                }
                            }
                        },
                        onResolveCorrection = { reqId, approved, note ->
                            coroutineScope.launch {
                                try {
                                    repository.resolveCorrectionRequest(reqId, approved, note)
                                    snackbarHostState.showSnackbar(if (approved) "Talep onaylandı." else "Talep reddedildi.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                                }
                            }
                        },
                        onUpdateSettings = { threshold, interval ->
                            coroutineScope.launch {
                                try {
                                    repository.updateSettings(threshold, interval)
                                    snackbarHostState.showSnackbar("Ayarlar başarıyla güncellendi.")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Hata: ${e.localizedMessage}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
