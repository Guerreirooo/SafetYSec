package pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddMonitor(
    onDismiss: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var monitorCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        userId?.let { uid ->
            val otp = (10000..99999).random().toString()
            val data = hashMapOf("code" to otp)
            firestore.collection("OneTimePass")
                .document(uid)
                .set(data)
                .addOnSuccessListener {
                    monitorCode = otp
                }
        }
    }

    if (monitorCode == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Dialog(
        onDismissRequest = {}
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {

                IconButton(
                    onClick = {
                        userId?.let { uid ->
                            val updates = hashMapOf<String, Any>(
                                "code" to com.google.firebase.firestore.FieldValue.delete(),
                                "createdAt" to com.google.firebase.firestore.FieldValue.delete()
                            )

                            firestore.collection("OneTimePass")
                                .document(uid)
                                .update(updates)
                                .addOnCompleteListener {
                                    onDismiss()
                                }
                        } ?: onDismiss()
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar"
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WARNING!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This is the code that your monitor will need to insert to complete the process. " +
                                "Do not close this box before the code has been filled in by the monitor.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = monitorCode!!,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}