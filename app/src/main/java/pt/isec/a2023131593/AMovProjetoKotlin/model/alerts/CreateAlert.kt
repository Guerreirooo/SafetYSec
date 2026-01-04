package pt.isec.a2023131593.AMovProjetoKotlin.model.alerts

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import pt.isec.a2023131593.AMovProjetoKotlin.R
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import kotlin.collections.get

fun createAlert(
    context: Context,
    userId: String,
    localizationNow: GeoPoint,
    type: AlertType
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("Relationship")
        .document(userId)
        .get()
        .addOnSuccessListener { doc ->
            val monitorIds = doc.get("monitor") as? List<String> ?: emptyList()

            val alertMap = hashMapOf(
                "codeWritted" to false,
                "timePassed" to false,
                "date" to Timestamp.now(),
                "location" to localizationNow,
                "monitorId" to monitorIds
            )

            db.collection("Alert")
                .document(userId)
                .set(mapOf(type.field to alertMap), SetOptions.merge())
                .addOnSuccessListener {
                    showLocalAlertNotification(context, type)
                    scheduleAlertCheck(context, userId, type)
                }
        }
}

fun showLocalAlertNotification(context: Context, type: AlertType) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "alert_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        manager.getNotificationChannel(channelId) == null
    ) {
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                context.getString(R.string.channel_emergency_name),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    val translatedType = getTranslatedAlertType(context, type.field)

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle(context.getString(R.string.notif_alert_detected))
        .setContentText(context.getString(R.string.notif_alert_body, translatedType))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun scheduleAlertCheck(context: Context, userId: String, type: AlertType) {
    val work = OneTimeWorkRequestBuilder<AlertCheckWorker>()
        .setInitialDelay(10, TimeUnit.SECONDS)
        .setInputData(
            workDataOf(
                "userId" to userId,
                "alertType" to type.field
            )
        )
        .build()

    WorkManager.getInstance(context).enqueue(work)
}

fun listenForAlerts(
    context: Context,
    monitorId: String,
    type: AlertType
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("Notification").document(monitorId)
        .addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val data = snapshot.data ?: return@addSnapshotListener

            data.forEach { (protectedId, rulesAny) ->
                val rules = rulesAny as? Map<*, *> ?: return@forEach
                val alertData = rules[type.field] as? Map<*, *> ?: return@forEach

                val dates = alertData["date"] as? List<*> ?: emptyList<Any>()
                val notifiedList = alertData["notified"] as? List<Boolean> ?: emptyList()

                val updatedNotifiedList = notifiedList.toMutableList()
                var needsUpdate = false

                notifiedList.forEachIndexed { index, isNotified ->
                    if (!isNotified) {
                        needsUpdate = true
                        updatedNotifiedList[index] = true

                        db.collection("User").document(protectedId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("Nome") ?: protectedId
                                showMonitorNotification(context, name, type)
                            }
                    }
                }

                if (needsUpdate) {
                    db.collection("Notification").document(monitorId)
                        .update("$protectedId.${type.field}.notified", updatedNotifiedList)
                }
            }
        }
}

fun clearAlert(
    transaction: Transaction,
    alertRef: DocumentReference,
    type: String
) {
    transaction.update(
        alertRef,
        mapOf(
            "$type.codeWritted" to FieldValue.delete(),
            "$type.timePassed" to FieldValue.delete(),
            "$type.date" to FieldValue.delete(),
            "$type.location" to FieldValue.delete(),
            "$type.monitorId" to FieldValue.delete()
        )
    )
}

fun showAlertCanceledNotification(context: Context, type: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "alert_channel"
    val translatedType = getTranslatedAlertType(context, type)

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle(context.getString(R.string.notif_alert_canceled_title))
        .setContentText(context.getString(R.string.notif_alert_canceled_body, translatedType))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun showMonitorNotification(context: Context, protectedName: String, type: AlertType) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "monitor_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        manager.getNotificationChannel(channelId) == null
    ) {
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                context.getString(R.string.channel_monitor_name),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    val translatedType = getTranslatedAlertType(context, type.field)

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle(context.getString(R.string.notif_monitor_title, translatedType.uppercase()))
        .setContentText(context.getString(R.string.notif_monitor_body, protectedName, translatedType))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun getTranslatedAlertType(context: Context, typeField: String): String {
    return when (typeField.lowercase()) {
        "fall" -> context.getString(R.string.alert_fall)
        "accident" -> context.getString(R.string.alert_accident)
        "location" -> context.getString(R.string.alert_geofencing)
        "inactivity" -> context.getString(R.string.alert_inactivity)
        "speed" -> context.getString(R.string.alert_speed)
        "panic" -> context.getString(R.string.alert_panic)
        else -> context.getString(R.string.alert_unknown)
    }
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(
    context: Context,
    onLocationReceived: (GeoPoint?) -> Unit
) {
    val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        null
    ).addOnSuccessListener { location ->
        onLocationReceived(
            location?.let { GeoPoint(it.latitude, it.longitude) }
        )
    }.addOnFailureListener {
        onLocationReceived(null)
    }
}

class AlertCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val alertType = inputData.getString("alertType") ?: return Result.failure()

        val db = FirebaseFirestore.getInstance()
        val alertRef = db.collection("Alert").document(userId)
        val historyRef = db.collection("History").document(userId)

        val doc = alertRef.get().await()
        val alert = doc.get(alertType) as? Map<*, *> ?: return Result.success()

        val codeWritted = alert["codeWritted"] as? Boolean ?: true
        val date = alert["date"] as? Timestamp ?: Timestamp.now()
        val location = alert["location"] as? GeoPoint ?: GeoPoint(0.0, 0.0)
        val monitorIds = alert["monitorId"] as? List<String> ?: emptyList()

        db.runTransaction { transaction ->
            val historyDoc = transaction.get(historyRef)
            val notificationDocs = monitorIds.associateWith { id ->
                transaction.get(db.collection("Notification").document(id))
            }
            val existing = historyDoc.get(alertType) as? Map<*, *>
            val existingDates = existing?.get("date") as? List<Timestamp> ?: emptyList()
            val existingLocations = existing?.get("location") as? List<GeoPoint> ?: emptyList()
            val existingCodes = existing?.get("codeWritted") as? List<Boolean> ?: emptyList()

            val newDates = existingDates + date
            val newLocations = existingLocations + location
            val newCodes = existingCodes + codeWritted

            transaction.set(
                historyRef,
                mapOf(
                    alertType to mapOf(
                        "codeWritted" to newCodes,
                        "date" to newDates,
                        "location" to newLocations
                    )
                ),
                SetOptions.merge()
            )

            if (codeWritted) {
                clearAlert(transaction, alertRef, alertType)
            } else {
                clearAlert(transaction, alertRef, alertType)

                monitorIds.forEach { monitorId ->
                    val notificationDoc = notificationDocs[monitorId]
                    val userData = notificationDoc?.get(userId) as? Map<*, *>
                    val alertData = userData?.get(alertType) as? Map<*, *>

                    val newDates =
                        (alertData?.get("date") as? List<Timestamp> ?: emptyList()) + date
                    val newLocations =
                        (alertData?.get("location") as? List<GeoPoint> ?: emptyList()) + location
                    val newNotified =
                        (alertData?.get("notified") as? List<Boolean> ?: emptyList()) + false

                    val updateData = mapOf(
                        userId to mapOf(
                            alertType to mapOf(
                                "date" to newDates,
                                "location" to newLocations,
                                "notified" to newNotified
                            )
                        )
                    )
                    transaction.set(
                        db.collection("Notification").document(monitorId),
                        updateData,
                        SetOptions.merge()
                    )
                }
            }
        }.await()

        return Result.success()
    }
}
