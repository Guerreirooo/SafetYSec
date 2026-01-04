package pt.isec.a2023131593.AMovProjetoKotlin.ui.home

import android.Manifest
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
import pt.isec.a2023131593.AMovProjetoKotlin.model.AlertType
import pt.isec.a2023131593.AMovProjetoKotlin.model.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.model.listenForAlerts
import pt.isec.a2023131593.AMovProjetoKotlin.model.rememberPermissionsState
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.other.CancelAlert
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf("SafetYSec") }

    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid
    val context = LocalContext.current

    var monitors by remember { mutableStateOf<List<String>>(emptyList()) }
    var protected by remember { mutableStateOf<List<String>>(emptyList()) }

    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val (hasRequiredPermissions, permissionLauncher) =
        rememberPermissionsState(permissions)

    LaunchedEffect(userId) {
        userId?.let { uid ->
            listenForAlerts(context, uid, AlertType.PANIC)
            listenForAlerts(context, uid, AlertType.FALL)
            listenForAlerts(context, uid, AlertType.ACCIDENT)
            listenForAlerts(context, uid, AlertType.SPEED)
            listenForAlerts(context, uid, AlertType.GEOFENCING)
            listenForAlerts(context, uid, AlertType.INACTIVITY)
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

    val scrollState = rememberScrollState()

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
                onAddProtectedClick = { showAddProtected = true },
                onCancelAlertClick = { showCancelAlert = true }
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
                    selectedRoute = Routes.DASHBOARD,
                    hasRequiredPermissions = hasRequiredPermissions,
                    requestPermissions = { permissionLauncher.launch(permissions.toTypedArray()) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text("Monitors:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (monitors.isEmpty()) {
                    Text("No monitors associated")
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

                Text("Protecteds:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (protected.isEmpty()) {
                    Text("No protecteds associated")
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
                                                snapshot.get("protected") as? List<String>
                                                    ?: emptyList()
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

    if (showCancelAlert) {
        CancelAlert(onDismiss = { showCancelAlert = false })
    }
}