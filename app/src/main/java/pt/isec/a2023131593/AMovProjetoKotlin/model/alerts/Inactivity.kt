package pt.isec.a2023131593.AMovProjetoKotlin.model.alerts

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import java.util.Calendar
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.get
import kotlin.math.sqrt

object InactivityRule {

    private var lastMovementTime = System.currentTimeMillis()
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var firstMeasurement = true

    private val monitorsInactivity = mutableMapOf<String, Long>()
    private val lastAlertSent = mutableMapOf<String, Long>()

    private var sensorListener: SensorEventListener? = null
    private var checkHandler: Handler? = null
    private var checkRunnable: Runnable? = null

    fun startMonitoring(
        context: Context,
        protectedId: String,
        onInactivityDetected: (List<String>) -> Unit
    ) {
        stopMonitoring(context)

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val threshold = 0.2f

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                if (firstMeasurement) {
                    lastX = x; lastY = y; lastZ = z
                    firstMeasurement = false
                    return
                }

                val delta = sqrt(
                    (x - lastX) * (x - lastX) +
                            (y - lastY) * (y - lastY) +
                            (z - lastZ) * (z - lastZ)
                )

                if (delta > threshold) {
                    lastMovementTime = System.currentTimeMillis()
                }

                lastX = x; lastY = y; lastZ = z
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        fetchMonitorsInactivityRules(protectedId)

        checkHandler = Handler(Looper.getMainLooper())
        val checkInterval = 30_000L

        checkRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val elapsedMinutes = (now - lastMovementTime) / 1000 / 60

                val triggered = monitorsInactivity.filter { (monitorId, allowedMinutes) ->
                    elapsedMinutes >= allowedMinutes &&
                            now - (lastAlertSent[monitorId] ?: 0L) > allowedMinutes * 60_000
                }.keys.toList()

                if (triggered.isNotEmpty()) {
                    triggered.forEach { lastAlertSent[it] = now }
                    onInactivityDetected(triggered)
                }

                checkHandler?.postDelayed(this, checkInterval)
            }
        }

        checkRunnable?.let { checkHandler?.post(it) }
    }

    fun stopMonitoring(context: Context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorListener?.let {
            sensorManager.unregisterListener(it)
            sensorListener = null
        }

        checkRunnable?.let {
            checkHandler?.removeCallbacks(it)
            checkRunnable = null
        }
        checkHandler = null

        monitorsInactivity.clear()
        lastAlertSent.clear()
        firstMeasurement = true
        lastMovementTime = System.currentTimeMillis()
    }

    private fun fetchMonitorsInactivityRules(protectedId: String) {
        FirebaseFirestore.getInstance()
            .collection("Rule")
            .document(protectedId)
            .get()
            .addOnSuccessListener { doc ->
                val map = mutableMapOf<String, Long>()

                doc.data?.forEach { (monitorId, rulesAny) ->
                    val rules = rulesAny as? Map<*, *> ?: return@forEach
                    val inactivity = rules["INACTIVITY"] as? Map<*, *> ?: return@forEach
                    val allowed = inactivity["allowed"] as? Boolean ?: false
                    if (!allowed) return@forEach

                    val parameters = inactivity["parameters"] as? List<*> ?: return@forEach
                    val minutesRaw = parameters.getOrNull(0)
                    val minutes = when (minutesRaw) {
                        is Long -> minutesRaw
                        is String -> minutesRaw.toLongOrNull()
                        else -> null
                    } ?: return@forEach

                    map[monitorId] = minutes
                }

                monitorsInactivity.clear()
                monitorsInactivity.putAll(map)
            }
    }
}

fun createInactivityAlert(
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
                val fallRules = monitorRules["INACTIVITY"] as? Map<*, *> ?: return@mapNotNull null
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
                .set(mapOf(AlertType.INACTIVITY.field to alertMap), SetOptions.merge())
                .addOnSuccessListener {
                    showLocalAlertNotification(context, AlertType.INACTIVITY)
                    scheduleAlertCheck(context, protectedId, AlertType.INACTIVITY)
                }
        }
}