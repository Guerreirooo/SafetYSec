package pt.isec.a2023131593.AMovProjetoKotlin.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import pt.isec.a2023131593.AMovProjetoKotlin.model.Routes

@Composable
fun MonitorCard(
    monitorUid: String,
    navController: NavHostController,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onMonitorRemoved: () -> Unit = {}
) {
    var nome by remember { mutableStateOf("") }
    var telemovel by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(monitorUid) {
        firestore.collection("User")
            .document(monitorUid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    nome = document.getString("Nome") ?: ""
                    telemovel = document.getString("Telemovel") ?: ""
                }
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(nome, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(telemovel, style = MaterialTheme.typography.bodyMedium)
                }

                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Remove, contentDescription = "Remover Monitor")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Opções Monitorização",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    navController.navigate(
                        "${Routes.MONITOR_RULES}/$monitorUid"
                    )
                }
            )
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    IconButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tem a certeza que quer remover este monitor?",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                                currentUserId?.let { uid ->
                                    val userRef = firestore.collection("Relationship").document(uid)
                                    val monitorRef = firestore.collection("Relationship").document(monitorUid)

                                    userRef.update("monitor", FieldValue.arrayRemove(monitorUid))
                                        .addOnSuccessListener {
                                            monitorRef.update("protected", FieldValue.arrayRemove(uid))
                                                .addOnSuccessListener {
                                                    showDialog = false
                                                    onMonitorRemoved()
                                                }
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }
}