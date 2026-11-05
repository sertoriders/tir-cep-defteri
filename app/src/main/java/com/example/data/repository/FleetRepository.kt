package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Advance
import com.example.data.model.AppSettings
import com.example.data.model.CorrectionRequest
import com.example.data.model.DebtRecord
import com.example.data.model.ExpenseEntry
import com.example.data.model.FuelEntry
import com.example.data.model.Trip
import com.example.data.model.UserProfile
import com.example.data.model.Vehicle
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FleetRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("İşlem için kullanıcı girişi gereklidir.")
    }

    // ----------------- ARAÇLAR (VEHICLES) -----------------

    suspend fun bootstrapInitialData() {
        val initialVehicles = listOf(
            Vehicle(
                id = "ford_cargo_1838",
                plate = "34 KAR 1838",
                name = "2014 Ford Cargo 1838 – 13.60 Açık Kasa",
                tankCapacity = 600.0,
                currentFuelLiters = 450.0,
                avgConsumptionLoaded = 33.0,
                avgConsumptionEmpty = 26.0,
                odometerKm = 385400.0,
                status = "IDLE",
                lastLat = 41.0082,
                lastLng = 28.9784,
                lastLocationAddress = "İstanbul Hadımköy Garajı"
            ),
            Vehicle(
                id = "ford_fmax_2024",
                plate = "34 FMX 2024",
                name = "2024 Ford F-MAX – 13.60 Açık Kasa",
                tankCapacity = 600.0,
                currentFuelLiters = 520.0,
                avgConsumptionLoaded = 31.0,
                avgConsumptionEmpty = 24.5,
                odometerKm = 42100.0,
                status = "IDLE",
                lastLat = 39.9334,
                lastLng = 32.8597,
                lastLocationAddress = "Ankara Lojistik Üssü"
            ),
            Vehicle(
                id = "renault_premium_430",
                plate = "34 REN 430",
                name = "Renault Premium 430 – 13.60 Açık Kasa",
                tankCapacity = 600.0,
                currentFuelLiters = 380.0,
                avgConsumptionLoaded = 34.0,
                avgConsumptionEmpty = 27.0,
                odometerKm = 612800.0,
                status = "IDLE",
                lastLat = 38.4237,
                lastLng = 27.1428,
                lastLocationAddress = "İzmir Bornova Nakliyeciler Sitesi"
            )
        )

        for (vehicle in initialVehicles) {
            val docRef = db.collection("vehicles").document(vehicle.id)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val data = mapOf(
                    "id" to vehicle.id,
                    "plate" to vehicle.plate,
                    "name" to vehicle.name,
                    "assignedDriverId" to vehicle.assignedDriverId,
                    "assignedDriverName" to vehicle.assignedDriverName,
                    "tankCapacity" to vehicle.tankCapacity,
                    "currentFuelLiters" to vehicle.currentFuelLiters,
                    "avgConsumptionLoaded" to vehicle.avgConsumptionLoaded,
                    "avgConsumptionEmpty" to vehicle.avgConsumptionEmpty,
                    "odometerKm" to vehicle.odometerKm,
                    "status" to vehicle.status,
                    "lastLat" to vehicle.lastLat,
                    "lastLng" to vehicle.lastLng,
                    "lastLocationAddress" to vehicle.lastLocationAddress,
                    "lastLocationTime" to FieldValue.serverTimestamp(),
                    "activeTripId" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    docRef.set(data).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, docRef.path)
                }
            }
        }
    }

    fun observeVehicles(): Flow<List<Vehicle>> =
        db.collection("vehicles").snapshots().map { snapshot ->
            snapshot.toObjects(Vehicle::class.java)
        }

    fun observeVehicle(vehicleId: String): Flow<Vehicle?> =
        db.collection("vehicles").document(vehicleId).snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(Vehicle::class.java) else null
        }

    suspend fun getVehicle(vehicleId: String): Vehicle? {
        val snapshot = db.collection("vehicles").document(vehicleId).get().await()
        return if (snapshot.exists()) snapshot.toObject(Vehicle::class.java) else null
    }

    suspend fun assignDriverToVehicle(vehicleId: String, driverId: String, driverName: String) {
        val docRef = db.collection("vehicles").document(vehicleId)
        val data = mapOf(
            "assignedDriverId" to driverId,
            "assignedDriverName" to driverName,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(data).await()
            // Ayrıca kullanıcının profilini de güncelle
            if (driverId.isNotBlank()) {
                db.collection("users").document(driverId).update("assignedVehicleId", vehicleId).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateVehicleLocation(
        vehicleId: String,
        latitude: Double,
        longitude: Double,
        address: String,
        gpsKmDelta: Double = 0.0
    ) {
        val docRef = db.collection("vehicles").document(vehicleId)
        val updates = mutableMapOf<String, Any>(
            "lastLat" to latitude,
            "lastLng" to longitude,
            "lastLocationAddress" to address,
            "lastLocationTime" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()

            // Eğer aracın aktif bir seferi varsa seferin GPS kilometresini de artır
            val vehicle = docRef.get().await().toObject(Vehicle::class.java)
            if (vehicle != null && vehicle.activeTripId.isNotBlank() && gpsKmDelta > 0.0) {
                val tripRef = db.collection("trips").document(vehicle.activeTripId)
                tripRef.update(
                    mapOf(
                        "gpsRouteKm" to FieldValue.increment(gpsKmDelta),
                        "endLat" to latitude,
                        "endLng" to longitude,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun setVehicleRestMode(vehicleId: String, isResting: Boolean) {
        val docRef = db.collection("vehicles").document(vehicleId)
        val updates = mutableMapOf<String, Any>(
            "isResting" to isResting,
            "status" to if (isResting) "RESTING" else "IDLE",
            "unrecordedDriveAlert" to false,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isResting) {
            updates["restStartTime"] = FieldValue.serverTimestamp()
        }
        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun setUnrecordedDriveAlert(vehicleId: String, alert: Boolean) {
        val docRef = db.collection("vehicles").document(vehicleId)
        try {
            docRef.update(
                mapOf(
                    "unrecordedDriveAlert" to alert,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    // ----------------- SEFERLER (TRIPS) -----------------

    fun observeTrips(): Flow<List<Trip>> =
        db.collection("trips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(Trip::class.java) }

    fun observeDriverTrips(driverId: String): Flow<List<Trip>> =
        db.collection("trips")
            .whereEqualTo("driverId", driverId)
            .snapshots()
            .map { it.toObjects(Trip::class.java).sortedByDescending { trip -> trip.createdAt } }

    suspend fun startTrip(
        vehicleId: String,
        tripType: String, // "LOADED" veya "EMPTY"
        origin: String,
        destination: String,
        startKm: Double,
        freightPrice: Double = 0.0,
        paymentType: String = "VADELİ",
        paymentDueDate: String = "",
        startLat: Double = 0.0,
        startLng: Double = 0.0
    ): String {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid).get().await()
        val driverName = userDoc.getString("name") ?: auth.currentUser?.displayName ?: "Şoför"

        val timeStampFormat = SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())
        val tripId = "SFR-$timeStampFormat"
        val tripNumber = tripId

        val tripPayload = mapOf(
            "id" to tripId,
            "tripNumber" to tripNumber,
            "vehicleId" to vehicleId,
            "driverId" to uid,
            "driverName" to driverName,
            "tripType" to tripType,
            "origin" to origin,
            "destination" to destination,
            "startKm" to startKm,
            "endKm" to startKm,
            "gpsRouteKm" to 0.0,
            "freightPrice" to freightPrice,
            "isPaid" to false,
            "paymentType" to paymentType,
            "paymentDueDate" to paymentDueDate,
            "status" to "ACTIVE",
            "startTime" to FieldValue.serverTimestamp(),
            "startLat" to startLat,
            "startLng" to startLng,
            "endLat" to startLat,
            "endLng" to startLng,
            "kmDiscrepancy" to 0.0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val tripDoc = db.collection("trips").document(tripId)
        val vehicleDoc = db.collection("vehicles").document(vehicleId)

        try {
            tripDoc.set(tripPayload).await()
            vehicleDoc.update(
                mapOf(
                    "status" to tripType,
                    "activeTripId" to tripId,
                    "odometerKm" to startKm,
                    "isResting" to false,
                    "unrecordedDriveAlert" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return tripId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, tripDoc.path)
            throw e
        }
    }

    suspend fun endTrip(
        tripId: String,
        vehicleId: String,
        endKm: Double,
        endLat: Double = 0.0,
        endLng: Double = 0.0
    ) {
        val tripDoc = db.collection("trips").document(tripId)
        val vehicleDoc = db.collection("vehicles").document(vehicleId)

        val tripSnapshot = tripDoc.get().await()
        val startKm = tripSnapshot.getDouble("startKm") ?: endKm
        val gpsKm = tripSnapshot.getDouble("gpsRouteKm") ?: 0.0
        val odometerDiff = (endKm - startKm).coerceAtLeast(0.0)
        val discrepancy = Math.abs(odometerDiff - gpsKm)

        try {
            tripDoc.update(
                mapOf(
                    "status" to "COMPLETED",
                    "endKm" to endKm,
                    "endTime" to FieldValue.serverTimestamp(),
                    "endLat" to endLat,
                    "endLng" to endLng,
                    "kmDiscrepancy" to discrepancy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            vehicleDoc.update(
                mapOf(
                    "status" to "IDLE",
                    "activeTripId" to "",
                    "odometerKm" to endKm,
                    "isResting" to false,
                    "unrecordedDriveAlert" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, tripDoc.path)
            throw e
        }
    }

    suspend fun toggleTripPayment(tripId: String, currentPaid: Boolean) {
        val docRef = db.collection("trips").document(tripId)
        try {
            docRef.update(
                mapOf(
                    "isPaid" to !currentPaid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // ----------------- YAKIT (FUEL) -----------------

    fun observeFuelEntries(): Flow<List<FuelEntry>> =
        db.collection("fuel_entries")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(FuelEntry::class.java) }

    fun observeDriverFuelEntries(driverId: String): Flow<List<FuelEntry>> =
        db.collection("fuel_entries")
            .whereEqualTo("driverId", driverId)
            .snapshots()
            .map { it.toObjects(FuelEntry::class.java).sortedByDescending { f -> f.createdAt } }

    suspend fun addFuelEntry(
        vehicleId: String,
        tripId: String,
        liters: Double,
        pricePerLiter: Double,
        locationAddress: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        receiptUrl: String = ""
    ) {
        val uid = requireUserId()
        val totalAmount = liters * pricePerLiter
        val fuelId = UUID.randomUUID().toString()

        val docRef = db.collection("fuel_entries").document(fuelId)
        val payload = mapOf(
            "id" to fuelId,
            "vehicleId" to vehicleId,
            "driverId" to uid,
            "tripId" to tripId,
            "liters" to liters,
            "pricePerLiter" to pricePerLiter,
            "totalAmount" to totalAmount,
            "locationAddress" to locationAddress,
            "latitude" to latitude,
            "longitude" to longitude,
            "receiptUrl" to receiptUrl,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            // Araçtaki tahmini yakıt miktarını güncelle
            val vehicleDoc = db.collection("vehicles").document(vehicleId)
            vehicleDoc.update(
                mapOf(
                    "currentFuelLiters" to FieldValue.increment(liters),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // ----------------- MASRAFLAR (EXPENSES) -----------------

    fun observeExpenseEntries(): Flow<List<ExpenseEntry>> =
        db.collection("expense_entries")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(ExpenseEntry::class.java) }

    fun observeDriverExpenseEntries(driverId: String): Flow<List<ExpenseEntry>> =
        db.collection("expense_entries")
            .whereEqualTo("driverId", driverId)
            .snapshots()
            .map { it.toObjects(ExpenseEntry::class.java).sortedByDescending { e -> e.createdAt } }

    suspend fun addExpenseEntry(
        vehicleId: String,
        tripId: String,
        category: String,
        amount: Double,
        description: String,
        locationAddress: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        receiptUrl: String = ""
    ) {
        val uid = requireUserId()
        val expenseId = UUID.randomUUID().toString()
        val docRef = db.collection("expense_entries").document(expenseId)

        val payload = mapOf(
            "id" to expenseId,
            "vehicleId" to vehicleId,
            "driverId" to uid,
            "tripId" to tripId,
            "category" to category,
            "amount" to amount,
            "description" to description,
            "locationAddress" to locationAddress,
            "latitude" to latitude,
            "longitude" to longitude,
            "receiptUrl" to receiptUrl,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // ----------------- DÜZELTME TALEPLERİ (CORRECTION REQUESTS) -----------------

    fun observeCorrectionRequests(): Flow<List<CorrectionRequest>> =
        db.collection("correction_requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(CorrectionRequest::class.java) }

    fun observeDriverCorrectionRequests(driverId: String): Flow<List<CorrectionRequest>> =
        db.collection("correction_requests")
            .whereEqualTo("driverId", driverId)
            .snapshots()
            .map { it.toObjects(CorrectionRequest::class.java).sortedByDescending { r -> r.createdAt } }

    suspend fun submitCorrectionRequest(
        targetType: String,
        targetId: String,
        vehicleId: String,
        requestedChanges: String
    ) {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid).get().await()
        val driverName = userDoc.getString("name") ?: auth.currentUser?.displayName ?: "Şoför"

        val reqId = UUID.randomUUID().toString()
        val docRef = db.collection("correction_requests").document(reqId)

        val payload = mapOf(
            "id" to reqId,
            "driverId" to uid,
            "driverName" to driverName,
            "vehicleId" to vehicleId,
            "targetType" to targetType,
            "targetId" to targetId,
            "requestedChanges" to requestedChanges,
            "status" to "PENDING",
            "adminNote" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun resolveCorrectionRequest(reqId: String, approved: Boolean, adminNote: String) {
        val docRef = db.collection("correction_requests").document(reqId)
        val status = if (approved) "APPROVED" else "REJECTED"
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "adminNote" to adminNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // ----------------- AVANSLAR (ADVANCES) -----------------

    fun observeAdvances(): Flow<List<Advance>> =
        db.collection("advances")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(Advance::class.java) }

    fun observeDriverAdvances(driverId: String): Flow<List<Advance>> =
        db.collection("advances")
            .whereEqualTo("driverId", driverId)
            .snapshots()
            .map { it.toObjects(Advance::class.java).sortedByDescending { a -> a.createdAt } }

    suspend fun addAdvance(
        driverId: String,
        driverName: String,
        amount: Double,
        description: String,
        date: String
    ) {
        val advId = UUID.randomUUID().toString()
        val docRef = db.collection("advances").document(advId)
        val payload = mapOf(
            "id" to advId,
            "driverId" to driverId,
            "driverName" to driverName,
            "amount" to amount,
            "description" to description,
            "date" to date,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // ----------------- ALACAK / VERECEK (DEBTS) -----------------

    fun observeDebtRecords(): Flow<List<DebtRecord>> =
        db.collection("debt_records")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(DebtRecord::class.java) }

    suspend fun addDebtRecord(
        type: String, // RECEIVABLE veya PAYABLE
        companyOrPerson: String,
        totalAmount: Double,
        dueDate: String,
        paymentFrequency: String = "ONCE",
        notes: String = ""
    ) {
        val debtId = UUID.randomUUID().toString()
        val docRef = db.collection("debt_records").document(debtId)
        val payload = mapOf(
            "id" to debtId,
            "type" to type,
            "companyOrPerson" to companyOrPerson,
            "totalAmount" to totalAmount,
            "paidAmount" to 0.0,
            "remainingAmount" to totalAmount,
            "dueDate" to dueDate,
            "paymentFrequency" to paymentFrequency,
            "status" to "PENDING",
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordDebtPayment(debtId: String, paymentAmount: Double) {
        val docRef = db.collection("debt_records").document(debtId)
        val snapshot = docRef.get().await()
        val totalAmount = snapshot.getDouble("totalAmount") ?: 0.0
        val currentPaid = snapshot.getDouble("paidAmount") ?: 0.0
        val newPaid = currentPaid + paymentAmount
        val newRemaining = (totalAmount - newPaid).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.0) "PAID" else "PARTIAL"

        try {
            docRef.update(
                mapOf(
                    "paidAmount" to newPaid,
                    "remainingAmount" to newRemaining,
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // ----------------- KULLANICILAR & ROLLER (USERS) -----------------

    suspend fun getUserProfile(userId: String): UserProfile? {
        val doc = db.collection("users").document(userId).get().await()
        return if (doc.exists()) doc.toObject(UserProfile::class.java) else null
    }

    suspend fun syncOrCreateUserProfile(userId: String, email: String, name: String): UserProfile {
        val docRef = db.collection("users").document(userId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(UserProfile::class.java)!!
        }

        // Bootstrap admin: eğer email sh_dilek@hotmail.com ise direkt ADMIN
        val role = if (email.equals("sh_dilek@hotmail.com", ignoreCase = true)) "ADMIN" else "DRIVER"
        val newProfile = UserProfile(
            userId = userId,
            email = email,
            name = name,
            role = role,
            assignedVehicleId = if (role == "DRIVER") "ford_cargo_1838" else ""
        )
        val payload = mapOf(
            "userId" to userId,
            "email" to email,
            "name" to name,
            "role" to role,
            "assignedVehicleId" to newProfile.assignedVehicleId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
        return newProfile
    }

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users").snapshots().map { it.toObjects(UserProfile::class.java) }

    suspend fun updateUserRoleAndVehicle(userId: String, role: String, assignedVehicleId: String) {
        val docRef = db.collection("users").document(userId)
        try {
            docRef.update(
                mapOf(
                    "role" to role,
                    "assignedVehicleId" to assignedVehicleId
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // ----------------- AYARLAR (SETTINGS) -----------------

    fun observeSettings(): Flow<AppSettings> =
        db.collection("settings").document("app_settings").snapshots().map { snapshot ->
            if (snapshot.exists()) {
                AppSettings(
                    kmDiscrepancyThreshold = snapshot.getDouble("kmDiscrepancyThreshold") ?: 10.0,
                    gpsUpdateIntervalSeconds = snapshot.getLong("gpsUpdateIntervalSeconds") ?: 15L
                )
            } else {
                AppSettings()
            }
        }

    suspend fun updateSettings(threshold: Double, intervalSeconds: Long) {
        val docRef = db.collection("settings").document("app_settings")
        val payload = mapOf(
            "kmDiscrepancyThreshold" to threshold,
            "gpsUpdateIntervalSeconds" to intervalSeconds,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
