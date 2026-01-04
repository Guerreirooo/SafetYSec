package pt.isec.a2023131593.AMovProjetoKotlin.ui.alert

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.listenForAlerts
import pt.isec.a2023131593.AMovProjetoKotlin.model.rememberPermissionsState
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.HistoryItem
import pt.isec.a2023131593.AMovProjetoKotlin.R
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedInfo(
    protectedUid: String,
    navController: NavHostController
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val defaultTitle = stringResource(id = R.string.app_name)
    var selectedItem by remember { mutableStateOf(defaultTitle) }

    var nome by remember { mutableStateOf("") }
    var telemovel by remember { mutableStateOf("") }

    var alertCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var lastAlertType by remember { mutableStateOf<String?>(null) }
    var lastAlertDate by remember { mutableStateOf<Timestamp?>(null) }
    var lastAlertLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var historyItems by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }

    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val (hasRequiredPermissions, permissionLauncher) =
        rememberPermissionsState(permissions)

    LaunchedEffect(Unit) {
        if (!hasRequiredPermissions) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    LaunchedEffect(auth.currentUser?.uid) {
        auth.currentUser?.uid?.let { uid ->
            listenForAlerts(context, uid, AlertType.PANIC)
            listenForAlerts(context, uid, AlertType.FALL)
            listenForAlerts(context, uid, AlertType.ACCIDENT)
            listenForAlerts(context, uid, AlertType.SPEED)
            listenForAlerts(context, uid, AlertType.GEOFENCING)
            listenForAlerts(context, uid, AlertType.INACTIVITY)
        }
    }

    LaunchedEffect(protectedUid) {
        firestore.collection("User")
            .document(protectedUid)
            .get()
            .addOnSuccessListener { doc ->
                nome = doc.getString("Nome") ?: ""
                telemovel = doc.getString("Telemovel") ?: ""
            }

        firestore.collection("History")
            .document(protectedUid)
            .get()
            .addOnSuccessListener { doc ->
                val result = mutableListOf<HistoryItem>()
                doc.data?.forEach { (alertType, value) ->
                    val alertMap = value as? Map<*, *> ?: return@forEach

                    val dates = (alertMap["date"] as? List<*>)?.mapNotNull { it as? Timestamp } ?: emptyList()
                    val locations = (alertMap["location"] as? List<*>)?.mapNotNull { it as? GeoPoint } ?: emptyList()
                    val codes = (alertMap["codeWritted"] as? List<*>)?.mapNotNull { it as? Boolean } ?: List(dates.size) { true }

                    val size = minOf(dates.size, locations.size, codes.size)
                    for (i in 0 until size) {
                        val code = codes.getOrNull(i) ?: true
                        if (!code) {
                            result.add(
                                HistoryItem(
                                    type = alertType,
                                    date = dates[i],
                                    location = locations[i],
                                    codeWritted = code
                                )
                            )
                        }
                    }
                }
                historyItems = result.sortedByDescending { it.date }
                alertCounts = historyItems.groupingBy { it.type }.eachCount()
                historyItems.firstOrNull()?.let { last ->
                    lastAlertType = last.type
                    lastAlertDate = last.date
                    lastAlertLocation = last.location
                }
            }
    }

    if (!hasRequiredPermissions) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.error_permissions_required),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(permissions.toTypedArray()) }) {
                    Text(stringResource(id = R.string.btn_concede_permissions))
                }
            }
        }
        return
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
                    title = { Text(stringResource(id = R.string.title_protected_info)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.desc_menu))
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(
                    navController = navController,
                    selectedRoute = Routes.INFO,
                    hasRequiredPermissions = hasRequiredPermissions,
                    requestPermissions = { permissionLauncher.launch(permissions.toTypedArray()) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.label_name_field, nome),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(id = R.string.label_phone_field, telemovel),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.label_total_alerts),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (alertCounts.isEmpty()) {
                    Text(stringResource(id = R.string.label_no_alerts))
                } else {
                    alertCounts.forEach { (type, count) ->
                        Text("• $type: $count")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.label_last_alert),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                lastAlertLocation?.let { loc ->
                    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                    val naText = stringResource(id = R.string.not_available)

                    Card(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(id = R.string.label_type_field, lastAlertType ?: naText),
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                text = stringResource(
                                    id = R.string.label_date_field,
                                    lastAlertDate?.let { formatter.format(it.toDate()) } ?: naText
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(id = R.string.label_location_newest),
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val alertLatLng = LatLng(loc.latitude, loc.longitude)
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(alertLatLng, 15f)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState
                                ) {
                                    Marker(
                                        state = MarkerState(position = alertLatLng),
                                        title = stringResource(id = R.string.marker_last_alert)
                                    )
                                }
                            }
                        }
                    }
                } ?: Text(stringResource(id = R.string.label_no_alerts))
            }
        }
    }

    if (showAddMonitor) AddMonitor(onDismiss = { showAddMonitor = false })
    if (showAddProtected) AddProtected(onDismiss = { showAddProtected = false }, onProtectedAdded = {})
    if (showCancelAlert) CancelAlert(onDismiss = { showCancelAlert = false })
}
