package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.repository.FleetRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class TrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: FleetRepository

    private var currentVehicleId: String = ""
    private var lastLocation: Location? = null
    private var totalDistanceKm: Double = 0.0

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        repository = FleetRepository(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onNewLocation(location)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_TRACKING) {
            stopTracking()
            return START_NOT_STICKY
        }

        currentVehicleId = intent?.getStringExtra(EXTRA_VEHICLE_ID) ?: ""
        val initialPlate = intent?.getStringExtra(EXTRA_PLATE) ?: "TIR"

        val notification = buildNotification("Takip Aktif: $initialPlate", "Güzergâh ve GPS kaydı alınıyor...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startLocationUpdates()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            15000L // Hedef 15 saniye aralık
        ).apply {
            setMinUpdateIntervalMillis(10000L)
            setMinUpdateDistanceMeters(10f)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("TrackingService", "Konum izni eksik", e)
        }
    }

    private fun onNewLocation(location: Location) {
        val prev = lastLocation
        var deltaKm = 0.0
        if (prev != null) {
            val meters = prev.distanceTo(location)
            deltaKm = meters / 1000.0
            totalDistanceKm += deltaKm
        }
        lastLocation = location

        val addressText = resolveAddress(location.latitude, location.longitude)

        updateNotification(
            "Konum: ${String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)}",
            "Toplam Yol: ${String.format(Locale.getDefault(), "%.1f km", totalDistanceKm)} | $addressText"
        )

        val speedKmh = if (location.hasSpeed()) {
            location.speed * 3.6
        } else {
            deltaKm * 240.0 // approx 15 seconds interval
        }

        if (currentVehicleId.isNotBlank()) {
            serviceScope.launch {
                repository.updateVehicleLocation(
                    vehicleId = currentVehicleId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = addressText,
                    gpsKmDelta = deltaKm
                )

                // İstirahat modu veya kayıt dışı sürüş algılama:
                // Hız > 10 km/s veya 15 saniyede 50 metreden fazla yer değiştirme varsa
                if (speedKmh > 10.0 || deltaKm >= 0.05) {
                    val currentVehicle = repository.getVehicle(currentVehicleId)
                    if (currentVehicle != null) {
                        val isOutsideTrip = currentVehicle.activeTripId.isBlank()
                        val isResting = currentVehicle.isResting || currentVehicle.status == "RESTING"
                        if (isResting || isOutsideTrip) {
                            if (!currentVehicle.unrecordedDriveAlert) {
                                repository.setUnrecordedDriveAlert(currentVehicle.id, true)
                                sendUnrecordedDriveNotification(this@TrackingService, currentVehicle.plate)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resolveAddress(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale("tr", "TR"))
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val subAdmin = addr.subAdminArea ?: addr.adminArea ?: ""
                val locality = addr.locality ?: addr.subLocality ?: ""
                if (locality.isNotBlank() || subAdmin.isNotBlank()) {
                    "$locality, $subAdmin".trim(',', ' ')
                } else {
                    addr.getAddressLine(0) ?: "Enlem: ${String.format(Locale.US, "%.4f", latitude)}, Boylam: ${String.format(Locale.US, "%.4f", longitude)}"
                }
            } else {
                "Enlem: ${String.format(Locale.US, "%.4f", latitude)}, Boylam: ${String.format(Locale.US, "%.4f", longitude)}"
            }
        } catch (e: Exception) {
            "Enlem: ${String.format(Locale.US, "%.4f", latitude)}, Boylam: ${String.format(Locale.US, "%.4f", longitude)}"
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val trackingChannel = NotificationChannel(
                CHANNEL_ID,
                "TIR CEP DEFTERİ - Güzergâh Takibi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sefer boyunca araç GPS konumunu kaydeder ve şoföre takip edildiğini bildirir."
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "TIR CEP DEFTERİ - Sürüş ve Sefer Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "İstirahat veya kayıt dışı sürüş algılandığında şoföre sefer kaydı oluşturması için bildirim gönderir."
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(trackingChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TIR CEP DEFTERİ - $title")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "tir_tracking_channel"
        const val CHANNEL_ALERT_ID = "tir_alert_channel"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 2002
        const val ACTION_START_TRACKING = "com.example.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.action.STOP_TRACKING"
        const val EXTRA_VEHICLE_ID = "extra_vehicle_id"
        const val EXTRA_PLATE = "extra_plate"

        fun sendUnrecordedDriveNotification(context: Context, vehiclePlate: String = "") {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val alertChannel = NotificationChannel(
                    CHANNEL_ALERT_ID,
                    "TIR CEP DEFTERİ - Sürüş ve Sefer Uyarıları",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "İstirahat veya kayıt dışı sürüş algılandığında şoföre sefer kaydı oluşturması için bildirim gönderir."
                    enableVibration(true)
                }
                manager.createNotificationChannel(alertChannel)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = if (launchIntent != null) {
                android.app.PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val plateText = if (vehiclePlate.isNotBlank()) " ($vehiclePlate)" else ""
            val builder = NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
                .setContentTitle("⚠️ SÜRÜŞ ALGILANDI: Sefer Kaydı Oluşturun!")
                .setContentText("Aktif sefer kaydınız bulunmuyor$plateText. Lütfen sefer kaydı başlatın.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Aracınızda hareket algılandı ancak aktif bir sefer kaydınız yok$plateText. İstirahat modu veya garaj dışına çıkıyorsanız lütfen 'Yükü Aldım' veya 'Boş Seyir' sefer kaydı oluşturun.")
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            if (pendingIntent != null) {
                builder.setContentIntent(pendingIntent)
            }

            manager.notify(ALERT_NOTIFICATION_ID, builder.build())
        }

        fun start(context: Context, vehicleId: String, plate: String) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_VEHICLE_ID, vehicleId)
                putExtra(EXTRA_PLATE, plate)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
