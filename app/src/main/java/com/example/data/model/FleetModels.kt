package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Vehicle(
    val id: String = "",
    val plate: String = "",
    val name: String = "",
    val assignedDriverId: String = "",
    val assignedDriverName: String = "",
    val tankCapacity: Double = 600.0,
    val currentFuelLiters: Double = 400.0,
    val avgConsumptionLoaded: Double = 33.0, // 33 L / 100km
    val avgConsumptionEmpty: Double = 26.0,  // 26 L / 100km
    val odometerKm: Double = 100000.0,
    val status: String = "IDLE", // IDLE, LOADED, EMPTY, MAINTENANCE, RESTING
    val lastLat: Double = 41.0082,
    val lastLng: Double = 28.9784,
    val lastLocationAddress: String = "İstanbul, Türkiye",
    @ServerTimestamp val lastLocationTime: Timestamp? = null,
    val activeTripId: String = "",
    val isResting: Boolean = false,
    @ServerTimestamp val restStartTime: Timestamp? = null,
    val unrecordedDriveAlert: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Trip(
    val id: String = "",
    val tripNumber: String = "",
    val vehicleId: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val tripType: String = "LOADED", // LOADED, EMPTY
    val origin: String = "",
    val destination: String = "",
    val startKm: Double = 0.0,
    val endKm: Double = 0.0,
    val gpsRouteKm: Double = 0.0,
    val freightPrice: Double = 0.0,
    val isPaid: Boolean = false,
    val paymentType: String = "VADELİ", // NAKİT, HAVALE, VADELİ
    val paymentDueDate: String = "",
    val status: String = "ACTIVE", // ACTIVE, COMPLETED
    @ServerTimestamp val startTime: Timestamp? = null,
    @ServerTimestamp val endTime: Timestamp? = null,
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val kmDiscrepancy: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FuelEntry(
    val id: String = "",
    val vehicleId: String = "",
    val driverId: String = "",
    val tripId: String = "",
    val liters: Double = 0.0,
    val pricePerLiter: Double = 0.0,
    val totalAmount: Double = 0.0,
    val locationAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val receiptUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExpenseEntry(
    val id: String = "",
    val vehicleId: String = "",
    val driverId: String = "",
    val tripId: String = "",
    val category: String = "YEMEK", // YEMEK, KAHVE, SIGARA, PARK, TAMIR, LASTIK, HGS, YIKAMA, DIGER
    val amount: Double = 0.0,
    val description: String = "",
    val locationAddress: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val receiptUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Advance(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DebtRecord(
    val id: String = "",
    val type: String = "RECEIVABLE", // RECEIVABLE (Alacak), PAYABLE (Verecek)
    val companyOrPerson: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val dueDate: String = "",
    val paymentFrequency: String = "ONCE", // ONCE, WEEKLY, MONTHLY
    val status: String = "PENDING", // PENDING, PARTIAL, PAID, OVERDUE
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CorrectionRequest(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val vehicleId: String = "",
    val targetType: String = "EXPENSE", // EXPENSE, FUEL, TRIP
    val targetId: String = "",
    val requestedChanges: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "DRIVER", // ADMIN, DRIVER
    val assignedVehicleId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppSettings(
    val kmDiscrepancyThreshold: Double = 10.0,
    val gpsUpdateIntervalSeconds: Long = 15L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
