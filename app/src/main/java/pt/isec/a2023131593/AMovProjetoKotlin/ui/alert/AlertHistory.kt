package pt.isec.a2023131593.AMovProjetoKotlin.ui.alert

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.HistoryItem
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.listenForAlerts
import pt.isec.a2023131593.AMovProjetoKotlin.model.rememberPermissionsState
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected
import pt.isec.a2023131593.AMovProjetoKotlin.R
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertHistory(
    navController: NavHostController,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val historyTitle = stringResource(id = R.string.title_alert_history)
    var selectedItem by remember { mutableStateOf(historyTitle) }

    var historyItems by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

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
        firestore.collection("History")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val result = mutableListOf<HistoryItem>()

                doc.data?.forEach { (alertType, value) ->
                    val alertMap = value as? Map<*, *> ?: return@forEach

                    val dates = alertMap["date"] as? List<Timestamp> ?: return@forEach
                    val locations = alertMap["location"] as? List<GeoPoint> ?: return@forEach
                    val codes = alertMap["codeWritted"] as? List<Boolean> ?: List(dates.size) { true }

                    for (i in dates.indices) {
                        result.add(
                            HistoryItem(
                                type = alertType,
                                date = dates[i],
                                location = locations[i],
                                codeWritted = codes.getOrNull(i) ?: true
                            )
                        )
                    }
                }

                historyItems = result.sortedByDescending { it.date }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
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
                onAddProtectedClick = { showAddProtected = true },
                onCancelAlertClick = { showCancelAlert = true }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(id = R.string.title_alert_history)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.desc_menu)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(
                    navController = navController,
                    selectedRoute = Routes.HISTORY,
                    hasRequiredPermissions = hasRequiredPermissions,
                    requestPermissions = { permissionLauncher.launch(permissions.toTypedArray()) }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (historyItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.msg_no_alerts_created),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(historyItems) { item ->
                                val translatedType = translateAlertType(item.type)
                                AlertHistoryCard(item.copy(type = translatedType))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMonitor) AddMonitor(onDismiss = { showAddMonitor = false })
    if (showAddProtected) AddProtected(onDismiss = { showAddProtected = false }, onProtectedAdded = {})
    if (showCancelAlert) CancelAlert(onDismiss = { showCancelAlert = false })
}

@Composable
fun translateAlertType(type: String): String {
    return when (type.uppercase()) {
        "FALL" -> stringResource(id = R.string.alert_fall)
        "ACCIDENT" -> stringResource(id = R.string.alert_accident)
        "GEOFENCING" -> stringResource(id = R.string.alert_geofencing)
        "INACTIVITY" -> stringResource(id = R.string.alert_inactivity)
        "SPEED" -> stringResource(id = R.string.alert_speed)
        "PANIC" -> stringResource(id = R.string.alert_panic)
        else -> stringResource(id = R.string.alert_unknown)
    }
}
