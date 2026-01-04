package pt.isec.a2023131593.AMovProjetoKotlin.rules


import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.scheduleAlertCheck
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.showLocalAlertNotification
import kotlin.collections.component1
import kotlin.collections.component2

object GeofencingRule {
    data class Geofence(
        val lat: Double,
        val lng: Double,
        val radius: Double
    )

    private val monitorsGeofence = mutableMapOf<String, Geofence>()
    private val lastAlertSent = mutableMapOf<String, Long>()
    private const val ALERT_COOLDOWN_MS = 60_000L
    private var locationCallback: LocationCallback? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startMonitoring(
        context: Context,
        protectedId: String,
        onGeofenceBroken: (List<String>) -> Unit
    ) {
        stopMonitoring(context)

        fetchGeofenceRules(protectedId)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        ).setMinUpdateDistanceMeters(10f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val now = System.currentTimeMillis()

                val triggered = monitorsGeofence.filter { (monitorId, geofence) ->
                    val distance = FloatArray(1)

                    Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        geofence.lat,
                        geofence.lng,
                        distance
                    )

                    val outside = distance[0] > geofence.radius
                    val cooldownOk =
                        now - (lastAlertSent[monitorId] ?: 0L) > ALERT_COOLDOWN_MS

                    outside && cooldownOk
                }.keys.toList()

                if (triggered.isNotEmpty()) {
                    triggered.forEach { lastAlertSent[it] = now }
                    onGeofenceBroken(triggered)
                }
            }
        }

        locationCallback?.let { callback ->
            fusedLocationClient?.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        }
    }

    fun stopMonitoring(context: Context) {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
        locationCallback = null
        fusedLocationClient = null
        monitorsGeofence.clear()
        lastAlertSent.clear()
    }

    private fun fetchGeofenceRules(protectedId: String) {
        FirebaseFirestore.getInstance()
            .collection("Rule")
            .document(protectedId)
            .get()
            .addOnSuccessListener { doc ->
                val map = mutableMapOf<String, Geofence>()
                doc.data?.forEach { (monitorId, rulesAny) ->
                    val rules = rulesAny as? Map<*, *> ?: return@forEach
                    val geoRule = rules["GEOFENCING"] as? Map<*, *> ?: return@forEach
                    if (!(geoRule["allowed"] as? Boolean ?: false)) return@forEach

                    val parameters = geoRule["parameters"] as? List<*> ?: return@forEach
                    if (parameters.size < 2) return@forEach

                    val coordsRaw = parameters[0] as? String ?: return@forEach
                    val latLng = parseCoordinates(coordsRaw) ?: return@forEach

                    val radiusRaw = parameters[1] as? String ?: return@forEach
                    val radius = radiusRaw.toDoubleOrNull() ?: return@forEach

                    map[monitorId] = Geofence(latLng.first, latLng.second, radius)
                }
                monitorsGeofence.clear()
                monitorsGeofence.putAll(map)
            }
    }

    private fun parseCoordinates(raw: String): Pair<Double, Double>? {
        return try {
            val parts = raw.split(",")
            val lat = parts[0].replace("° N", "").replace("° S", "").trim().toDouble()
            val lng = parts[1].replace("° W", "").replace("° E", "").trim().toDouble()

            val finalLat = if (raw.contains("S")) -lat else lat
            val finalLng = if (raw.contains("W")) -lng else lng

            Pair(finalLat, finalLng)
        } catch (e: Exception) {
            null
        }
    }
}

fun createGeofencingAlert(
    context: Context,
    protectedId: String,
    locationNow: GeoPoint
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("Rule")
        .document(protectedId)
        .get()
        .addOnSuccessListener { rulesDoc ->

            val now = java.util.Calendar.getInstance()
            val currentDay = now.get(java.util.Calendar.DAY_OF_WEEK)
            val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

            val activeMonitors = rulesDoc.data?.mapNotNull { (monitorId, monitorRulesAny) ->

                val monitorRules = monitorRulesAny as? Map<*, *> ?: return@mapNotNull null
                val fallRules = monitorRules["GEOFENCING"] as? Map<*, *> ?: return@mapNotNull null
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
                .set(mapOf(AlertType.GEOFENCING.field to alertMap), SetOptions.merge())
                .addOnSuccessListener {
                    showLocalAlertNotification(context, AlertType.GEOFENCING)
                    scheduleAlertCheck(context, protectedId, AlertType.GEOFENCING)
                }
        }
}