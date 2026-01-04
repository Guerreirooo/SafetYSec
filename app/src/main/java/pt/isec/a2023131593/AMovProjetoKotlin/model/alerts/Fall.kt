package pt.isec.a2023131593.AMovProjetoKotlin.model.alerts

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import java.util.Calendar
import kotlin.collections.get
import kotlin.math.sqrt

object FallRule {
    private var lastFallDetected = 0L

    private var sensorListener: SensorEventListener? = null

    fun startMonitoring(context: Context, onFallDetected: () -> Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        stopMonitoring(context)

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

                if (gForce < 0.7f && System.currentTimeMillis() - lastFallDetected > 1000) {
                    lastFallDetected = System.currentTimeMillis()
                    onFallDetected()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopMonitoring(context: Context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorListener?.let {
            sensorManager.unregisterListener(it)
            sensorListener = null
        }
    }
}

fun createFallAlert(
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
                val fallRules = monitorRules["FALL"] as? Map<*, *> ?: return@mapNotNull null
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
                .set(mapOf(AlertType.FALL.field to alertMap), SetOptions.merge())
                .addOnSuccessListener {
                    showLocalAlertNotification(context, AlertType.FALL)
                    scheduleAlertCheck(context, protectedId, AlertType.FALL)
                }
        }
}