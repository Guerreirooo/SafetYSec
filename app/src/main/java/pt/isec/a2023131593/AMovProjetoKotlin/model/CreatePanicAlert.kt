import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.FirebaseFirestore
import androidx.work.*
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

fun createPanicAlert(context: Context, userId: String, localizationNow: GeoPoint) {
    val db = FirebaseFirestore.getInstance()

    db.collection("Relationship")
        .document(userId)
        .get()
        .addOnSuccessListener { doc ->
            val monitorIds = doc.get("monitor") as? List<String> ?: emptyList()

            val panicMap = hashMapOf(
                "codeWritted" to false,
                "timePassed" to false,
                "date" to Timestamp.now(),
                "location" to localizationNow,
                "monitorId" to monitorIds
            )

            db.collection("Alert")
                .document(userId)
                .set(mapOf("PANIC" to panicMap))
                .addOnSuccessListener {
                    showLocalAlertNotification(context)
                    scheduleAlertCheck(context, userId)
                }
        }
}

fun showLocalAlertNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "panic_channel"
    val channelName = "Alertas de Emergência"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificações de alertas PANIC"
            manager.createNotificationChannel(channel)
        }
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle("SafetYSec - ⚠️ ALERTA DETETADO ⚠️")
        .setContentText("Insira o código de segurança em 10 segundos ou os monitores serão notificados.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun scheduleAlertCheck(context: Context, userId: String) {
    val work = OneTimeWorkRequestBuilder<AlertCheckWorker>()
        .setInitialDelay(10, TimeUnit.SECONDS)
        .setInputData(workDataOf("userId" to userId))
        .build()

    WorkManager.getInstance(context).enqueue(work)
}

class AlertCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val db = FirebaseFirestore.getInstance()

        val doc = db.collection("Alert").document(userId).get().await()
        val panic = doc.get("PANIC") as? Map<*, *> ?: return Result.success()
        val codeWritted = panic["codeWritted"] as? Boolean ?: true

        if (!codeWritted) {
            db.collection("Alert")
                .document(userId)
                .update("PANIC.timePassed", true)
        } else {
            db.collection("Alert")
                .document(userId)
                .update(
                    mapOf(
                        "PANIC.codeWritted" to FieldValue.delete(),
                        "PANIC.timePassed" to FieldValue.delete(),
                        "PANIC.date" to FieldValue.delete(),
                        "PANIC.location" to FieldValue.delete(),
                        "PANIC.monitorId" to FieldValue.delete()
                    )
                )

            showAlertCanceledNotification(applicationContext, userId)
        }

        return Result.success()
    }
}

fun showAlertCanceledNotification(context: Context, userId: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "panic_channel"
    val channelName = "Alertas de Emergência"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificações de alertas PANIC"
            manager.createNotificationChannel(channel)
        }
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle("SafetYSec - ALERTA CANCELADO")
        .setContentText("O seu alerta PANIC foi cancelado com sucesso!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}

fun listenForPanicAlerts(context: Context, monitorId: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("Alert")
        .addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot == null) return@addSnapshotListener

            for (doc in snapshot.documents) {
                val panic = doc.get("PANIC") as? Map<*, *> ?: continue
                val codeWritted = panic["codeWritted"] as? Boolean ?: true
                val timePassed = panic["timePassed"] as? Boolean ?: false
                val monitorIds = panic["monitorId"] as? List<*> ?: emptyList<Any>()

                if (!codeWritted && timePassed && monitorIds.contains(monitorId)) {
                    val protectedId = doc.id
                    db.collection("User").document(protectedId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val protectedName = userDoc.getString("Nome") ?: protectedId
                            showMonitorNotification(context, protectedName)
                        }
                }
            }
        }
}

fun showMonitorNotification(context: Context, protectedName: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "monitor_channel"
    val channelName = "Alertas de Monitores"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notificações de alertas para monitores"
            manager.createNotificationChannel(channel)
        }
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.mipmap.sym_def_app_icon)
        .setContentTitle("SafetYSec - ALERTA DO PROTEGIDO")
        .setContentText("O protegido $protectedName disparou um PANIC e não inseriu o código!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}
