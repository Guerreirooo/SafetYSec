package pt.isec.a2023131593.AMovProjetoKotlin.model.alerts

import android.Manifest
import android.content.Context
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import java.util.Calendar
import kotlin.collections.get

object SpeedRule {

    private val monitorsSpeed = mutableMapOf<String, Long>()
    private val lastAlertSent = mutableMapOf<String, Long>()
    private const val ALERT_COOLDOWN_MS = 60_000L
    private var locationCallback: LocationCallback? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startMonitoring(
        context: Context,
        protectedId: String,
        onSpeedDetected: (List<String>) -> Unit
    ) {
        stopMonitoring(context)

        val db = FirebaseFirestore.getInstance()

        db.collection("Rule")
            .document(protectedId)
            .get()
            .addOnSuccessListener { doc ->
                updateRules(doc)

                if (monitorsSpeed.isNotEmpty()) {
                    startLocationUpdates(context, onSpeedDetected)
                }
            }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates(context: Context, onSpeedDetected: (List<String>) -> Unit) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                val speedKmh = if (location.hasSpeed()) {
                    location.speed * 3.6
                } else {
                    0.0
                }

                val now = System.currentTimeMillis()

                val triggered = monitorsSpeed.filter { (monitorId, maxSpeed) ->
                    speedKmh > maxSpeed &&
                            now - (lastAlertSent[monitorId] ?: 0L) > ALERT_COOLDOWN_MS
                }.keys.toList()

                if (triggered.isNotEmpty()) {
                    triggered.forEach { lastAlertSent[it] = now }
                    onSpeedDetected(triggered)
                }
            }
        }

        locationCallback?.let {
            fusedLocationClient?.requestLocationUpdates(request, it, Looper.getMainLooper())
        }
    }

    fun stopMonitoring(context: Context) {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        locationCallback = null
        fusedLocationClient = null
        monitorsSpeed.clear()
        lastAlertSent.clear()
    }

    private fun updateRules(doc: DocumentSnapshot) {
        val map = mutableMapOf<String, Long>()
        doc.data?.forEach { (monitorId, rulesAny) ->
            val rules = rulesAny as? Map<*, *> ?: return@forEach
            val speedRule = rules["SPEED"] as? Map<*, *> ?: return@forEach
            if (!(speedRule["allowed"] as? Boolean ?: false)) return@forEach

            val parameters = speedRule["parameters"] as? List<*> ?: return@forEach
            val maxSpeedRaw = parameters.getOrNull(0)

            val maxSpeed = when (maxSpeedRaw) {
                is Long -> maxSpeedRaw
                is String -> maxSpeedRaw.toLongOrNull()
                is Int -> maxSpeedRaw.toLong()
                else -> null
            } ?: return@forEach

            map[monitorId] = maxSpeed
        }
        monitorsSpeed.clear()
        monitorsSpeed.putAll(map)
    }
}

fun createSpeedAlert(
    context: Context,
    protectedId: String,
    locationNow: GeoPoint
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("Rule")
        .document(protectedId)
        .get()
        .addOnSuccessListener { rulesDoc ->

            val now = Calendar.getInstance()
            val currentDay = now.get(Calendar.DAY_OF_WEEK)
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val activeMonitors = rulesDoc.data?.mapNotNull { (monitorId, monitorRulesAny) ->

                val monitorRules = monitorRulesAny as? Map<*, *> ?: return@mapNotNull null
                val fallRules = monitorRules["SPEED"] as? Map<*, *> ?: return@mapNotNull null
                val allowed = fallRules["allowed"] as? Boolean ?: false
                if (!allowed) return@mapNotNull null

                val scheduleDays = fallRules["scheduleDays"] as? List<*> ?: return@mapNotNull null

                var isActive = false
                var i = 0
                while (i + 2 < scheduleDays.size) {
                    val dayOfWeek = (scheduleDays[i] as? Number)?.toInt() ?: -1
                    val startStr = scheduleDays[i + 1] as? String ?: ""
                    val endStr = scheduleDays[i + 2] as? String ?: ""

                    if (dayOfWeek == currentDay) {
                        val startParts = startStr.split(":")
                        val endParts = endStr.split(":")

                        if (startParts.size == 2 && endParts.size == 2) {
                            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
                            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

                            if (currentMinutes in startMinutes..endMinutes) {
                                isActive = true
                                break
                            }
                        }
                    }
                    i += 3
                }

                if (isActive) monitorId else null

            } ?: emptyList()

            if (activeMonitors.isEmpty()) return@addOnSuccessListener

            val alertMap = hashMapOf(
                "codeWritted" to false,
                "timePassed" to false,
                "date" to Timestamp.now(),
                "location" to locationNow,
                "monitorId" to activeMonitors
            )

            db.collection("Alert")
                .document(protectedId)
                .set(mapOf(AlertType.SPEED.field to alertMap), SetOptions.merge())
                .addOnSuccessListener {
                    showLocalAlertNotification(context, AlertType.SPEED)
                    scheduleAlertCheck(context, protectedId, AlertType.SPEED)
                }
        }
}