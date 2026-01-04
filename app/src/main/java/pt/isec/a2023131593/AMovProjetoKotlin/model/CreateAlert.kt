package pt.isec.a2023131593.AMovProjetoKotlin.model

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
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle("SafetYSec - ⚠️ ALERT DETECTED ⚠️")
        .setContentText(
            "Alert ${type.title}. Insert the alert code within 10 seconds."
        )
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

        db.runTransaction { transaction ->
            val historyDoc = transaction.get(historyRef)
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
                transaction.update(alertRef, "$alertType.timePassed", true)
            }
        }

        return Result.success()
    }
}

fun listenForAlerts(
    context: Context,
    monitorId: String,
    type: AlertType
) {
    val db = FirebaseFirestore.getInstance()

    db.collection("Alert")
        .addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            for (doc in snapshot.documents) {
                val alert = doc.get(type.field) as? Map<*, *> ?: continue

                val codeWritted = alert["codeWritted"] as? Boolean ?: true
                val timePassed = alert["timePassed"] as? Boolean ?: false
                val monitorIds = alert["monitorId"] as? List<*> ?: emptyList<Any>()

                if (!codeWritted && timePassed && monitorIds.contains(monitorId)) {
                    val protectedId = doc.id

                    db.collection("User").document(protectedId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("Nome") ?: protectedId
                            showMonitorNotification(context, name, type)
                        }

                    clearAlert(db, protectedId, type.field)
                }
            }
        }
}

fun clearAlert(db: FirebaseFirestore, userId: String, type: String) {
    db.collection("Alert").document(userId).update(
        mapOf(
            "$type.codeWritted" to FieldValue.delete(),
            "$type.timePassed" to FieldValue.delete(),
            "$type.date" to FieldValue.delete(),
            "$type.location" to FieldValue.delete(),
            "$type.monitorId" to FieldValue.delete()
        )
    )
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        manager.getNotificationChannel(channelId) == null
    ) {
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle("SafetYSec - Canceled Alert")
        .setContentText("Alert $type has been successfully cancelled..")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun showMonitorNotification(
    context: Context,
    protectedName: String,
    type: AlertType
) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "monitor_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        manager.getNotificationChannel(channelId) == null
    ) {
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Monitor Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle("SafetYSec - ${type.title} ALERT")
        .setContentText("Protected $protectedName created an alert ${type.title}!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
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
