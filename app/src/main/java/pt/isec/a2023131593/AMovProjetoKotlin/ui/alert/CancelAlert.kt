package pt.isec.a2023131593.AMovProjetoKotlin.ui.alert

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.showAlertCanceledNotification
import pt.isec.a2023131593.AMovProjetoKotlin.R
import kotlin.collections.get
import kotlin.collections.iterator

enum class CancelResult {
    INVALID_CODE,
    NO_ALERTS,
    TIME_EXPIRED,
    SUCCESS
}

@Composable
fun CancelAlert(
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<CancelResult?>(null) }
    val applicationContext = LocalContext.current.applicationContext

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.desc_close)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = stringResource(id = R.string.title_cancel_alert),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(stringResource(id = R.string.label_alert_code)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (userId == null) return@Button

                            firestore.collection("User")
                                .document(userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val correctCode = userDoc.getString("CodigoAlerta")

                                    if (code != correctCode) {
                                        result = CancelResult.INVALID_CODE
                                        return@addOnSuccessListener
                                    }

                                    firestore.collection("Alert")
                                        .document(userId)
                                        .get()
                                        .addOnSuccessListener { alertDoc ->

                                            if (!alertDoc.exists()) {
                                                result = CancelResult.NO_ALERTS
                                                return@addOnSuccessListener
                                            }

                                            val data = alertDoc.data ?: run {
                                                result = CancelResult.NO_ALERTS
                                                return@addOnSuccessListener
                                            }

                                            var canceledAlertType: String? = null
                                            var hasTimePassedAlert = false

                                            for ((alertType, value) in data) {
                                                val alertMap = value as? Map<*, *> ?: continue

                                                val codeWritted = alertMap["codeWritted"] as? Boolean ?: true
                                                val timePassed = alertMap["timePassed"] as? Boolean ?: true

                                                if (!codeWritted && !timePassed) {
                                                    canceledAlertType = alertType
                                                    firestore.collection("Alert")
                                                        .document(userId)
                                                        .update("$alertType.codeWritted", true)
                                                    break
                                                } else if (!codeWritted && timePassed) {
                                                    hasTimePassedAlert = true
                                                }
                                            }

                                            result = when {
                                                canceledAlertType != null -> {
                                                    showAlertCanceledNotification(applicationContext, canceledAlertType)
                                                    CancelResult.SUCCESS
                                                }
                                                hasTimePassedAlert -> CancelResult.TIME_EXPIRED
                                                else -> CancelResult.NO_ALERTS
                                            }

                                            if (result == CancelResult.SUCCESS) onDismiss()
                                        }
                                }
                                .addOnFailureListener {
                                    result = CancelResult.INVALID_CODE
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.btn_confirm))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    result?.let {
                        val text = when (it) {
                            CancelResult.INVALID_CODE -> stringResource(id = R.string.error_invalid_code)
                            CancelResult.NO_ALERTS -> stringResource(id = R.string.error_no_alerts)
                            CancelResult.TIME_EXPIRED -> stringResource(id = R.string.error_time_expired)
                            CancelResult.SUCCESS -> ""
                        }

                        if (text.isNotEmpty()) {
                            Text(
                                text = text,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

