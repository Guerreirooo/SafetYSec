package pt.isec.a2023131593.AMovProjetoKotlin.model.alerts

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import pt.isec.a2023131593.AMovProjetoKotlin.rules.GeofencingRule
import pt.isec.a2023131593.AMovProjetoKotlin.rules.createGeofencingAlert
import kotlin.collections.get

object checkAllRules {

    private val db = FirebaseFirestore.getInstance()
    private var job: Job? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startMonitoring(context: Context, protectedId: String) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    checkAllRulesAndTriggerAlerts(context, protectedId)
                } catch (_: Exception) { }
                delay(3000L)
            }
        }

        FallRule.startMonitoring(context) {
            getCurrentLocation(context) { geoPoint ->
                if (geoPoint != null) {
                    createFallAlert(
                        context,
                        protectedId,
                        geoPoint
                    )
                }
            }
        }

        AccidentRule.startMonitoring(context) {
            getCurrentLocation(context) { geoPoint ->
                if (geoPoint != null) {
                    createAccidentAlert(
                        context,
                        protectedId,
                        geoPoint
                    )
                }
            }
        }

        InactivityRule.startMonitoring(context,protectedId) {
            getCurrentLocation(context) { geoPoint ->
                if (geoPoint != null) {
                    createInactivityAlert(context, protectedId, geoPoint)
                }
            }
        }

        SpeedRule.startMonitoring(context, protectedId) {
            getCurrentLocation(context) { geoPoint ->
                if (geoPoint != null) {
                    createSpeedAlert(context, protectedId, geoPoint)
                }
            }
        }

        GeofencingRule.startMonitoring(context, protectedId) {
            getCurrentLocation(context) { geoPoint ->
                if (geoPoint != null) {
                    createGeofencingAlert(
                        context,
                        protectedId,
                        geoPoint
                    )
                }
            }
        }
    }

    fun stopMonitoring(context: Context) {
        job?.cancel()
        job = null

        FallRule.stopMonitoring(context)
        AccidentRule.stopMonitoring(context)
        InactivityRule.stopMonitoring(context)
        SpeedRule.stopMonitoring(context)
        GeofencingRule.stopMonitoring(context)
    }

    private suspend fun checkAllRulesAndTriggerAlerts(context: Context, protectedId: String) {
        val userDoc = db.collection("Rule").document(protectedId).get().await()
        val rulesMap = userDoc.data ?: return

        val currentLocation = getCurrentLocationSuspended(context)

        rulesMap.forEach { (ruleName, value) ->
            val ruleData = value as? Map<*, *> ?: return@forEach
            val allowed = ruleData["allowed"] as? Boolean ?: false
            val parameters = ruleData["parameters"] as? List<String> ?: emptyList()

            if (!allowed) return@forEach

        }
    }

    private suspend fun getCurrentLocationSuspended(context: Context): GeoPoint? =
        suspendCancellableCoroutine { cont ->
            getCurrentLocation(context) { geo ->
                cont.resume(geo) {}
            }
        }
}