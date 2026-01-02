package pt.isec.a2023131593.AMovProjetoKotlin.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import listenForPanicAlerts
import pt.isec.a2023131593.AMovProjetoKotlin.model.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf("SafetYSec") }

    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    val context = LocalContext.current

    var monitors by remember { mutableStateOf<List<String>>(emptyList()) }
    var protected by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(userId) {
        userId?.let { uid ->
            listenForPanicAlerts(context, uid)
        }
    }

    LaunchedEffect(userId) {
        userId?.let { uid ->
            firestore.collection("Relationship")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    monitors = doc.get("monitor") as? List<String> ?: emptyList()
                    protected = doc.get("protected") as? List<String> ?: emptyList()
                }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LeftNavBar(
                navController = navController,
                drawerState = drawerState,
                scope = scope,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                onAddMonitorClick = { showAddMonitor = true },
                onAddProtectedClick = { showAddProtected = true }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(selectedItem) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(
                    navController = navController,
                    selectedRoute = Routes.DASHBOARD
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {

                Text("Monitores Atuais:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (monitors.isEmpty()) {
                    Text("Sem monitores associados")
                } else {
                    monitors.forEach { monitorUid ->
                        MonitorCard(
                            monitorUid = monitorUid,
                            navController = navController,
                            onMonitorRemoved = {
                                userId?.let { uid ->
                                    firestore.collection("Relationship")
                                        .document(uid)
                                        .get()
                                        .addOnSuccessListener { doc ->
                                            monitors =
                                                doc.get("monitor") as? List<String> ?: emptyList()
                                        }
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("Protegidos Atuais:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (protected.isEmpty()) {
                    Text("Sem protegidos associados")
                } else {
                    protected.forEach { protectedUid ->
                        ProtectedCard(
                            protectedUid = protectedUid,
                            navController = navController,
                            onProtectedRemoved = {
                                userId?.let { uid ->
                                    firestore.collection("Relationship")
                                        .document(uid)
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            protected =
                                                snapshot.get("protected") as? List<String> ?: emptyList()
                                        }
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showAddMonitor) {
        AddMonitor(onDismiss = { showAddMonitor = false })
    }

    if (showAddProtected) {
        AddProtected(
            onDismiss = { showAddProtected = false },
            onProtectedAdded = {
                userId?.let { uid ->
                    firestore.collection("Relationship")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            monitors = doc.get("monitor") as? List<String> ?: emptyList()
                            protected = doc.get("protected") as? List<String> ?: emptyList()
                        }
                }
            }
        )
    }
}

fun showMonitorNotification(
    context: Context,
    protectedName: String,
    eventType: String
) {
    val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channelId = "monitor_alert_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Alertas de Emergência",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("ALERTA DETETADO")
        .setContentText(
            "O protegido $protectedName sofreu um alerta do tipo $eventType"
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(System.currentTimeMillis().toInt(), notification)
}
