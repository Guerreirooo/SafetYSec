package pt.isec.a2023131593.AMovProjetoKotlin.ui.alert

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import pt.isec.a2023131593.AMovProjetoKotlin.model.rememberPermissionsState
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import pt.isec.a2023131593.AMovProjetoKotlin.R
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastAlert(
    protectedUid: String,
    navController: NavHostController
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var nome by remember { mutableStateOf("") }
    var telemovel by remember { mutableStateOf("") }
    val defaultTitle = stringResource(id = R.string.app_name)
    var selectedItem by remember { mutableStateOf(defaultTitle) }

    var lastAlertType by remember { mutableStateOf<String?>(null) }
    var lastAlertDate by remember { mutableStateOf<Timestamp?>(null) }
    var lastAlertLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var totalAlertsForThisMonitor by remember { mutableStateOf(0) }

    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val currentMonitorUid = auth.currentUser?.uid ?: ""

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val (hasRequiredPermissions, permissionLauncher) = rememberPermissionsState(permissions)

    if (!hasRequiredPermissions) {
        LaunchedEffect(Unit) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    LaunchedEffect(protectedUid, currentMonitorUid) {
        firestore.collection("User").document(protectedUid).get()
            .addOnSuccessListener { doc ->
                nome = doc.getString("Nome") ?: ""
                telemovel = doc.getString("Telemovel") ?: ""
            }

        firestore.collection("Notification").document(currentMonitorUid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val monitorData = doc.data ?: return@addOnSuccessListener
                val protectedMap = monitorData[protectedUid] as? Map<*, *> ?: return@addOnSuccessListener

                var newestDate: Timestamp? = null
                var newestLoc: GeoPoint? = null
                var newestType: String? = null
                var count = 0

                protectedMap.forEach { (type, content) ->
                    val alertTypeMap = content as? Map<*, *> ?: return@forEach
                    val dates = alertTypeMap["date"] as? List<Timestamp> ?: emptyList()
                    val locations = alertTypeMap["location"] as? List<GeoPoint> ?: emptyList()

                    count += dates.size

                    dates.forEachIndexed { index, timestamp ->
                        if (newestDate == null || timestamp.seconds > newestDate!!.seconds) {
                            newestDate = timestamp
                            newestType = type.toString()
                            newestLoc = locations.getOrNull(index)
                        }
                    }
                }

                lastAlertDate = newestDate
                lastAlertLocation = newestLoc
                lastAlertType = newestType
                totalAlertsForThisMonitor = count
            }
    }

    val scrollState = rememberScrollState()
    val naText = stringResource(id = R.string.not_available)

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
            )}
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(selectedItem) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.desc_menu)
                            )
                        }
                    }
                )},
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
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(scrollState)
            ) {
                Text(text = stringResource(id = R.string.label_protected_name, nome), style = MaterialTheme.typography.titleLarge)
                Text(text = stringResource(id = R.string.label_contact_phone, telemovel), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(id = R.string.total_alerts_received, totalAlertsForThisMonitor))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = stringResource(id = R.string.label_last_alert_sent_to_me), style = MaterialTheme.typography.titleMedium)

                lastAlertLocation?.let { loc ->
                    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

                    Card(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = stringResource(id = R.string.label_alert_type, lastAlertType ?: naText), style = MaterialTheme.typography.bodyLarge)
                            Text(text = stringResource(id = R.string.label_alert_date, lastAlertDate?.let { formatter.format(it.toDate()) } ?: naText))

                            Spacer(modifier = Modifier.height(16.dp))

                            val alertLatLng = LatLng(loc.latitude, loc.longitude)
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(alertLatLng, 15f)
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState
                                ) {
                                    Marker(state = MarkerState(position = alertLatLng), title = stringResource(id = R.string.marker_alert_location))
                                }
                            }
                        }
                    }
                } ?: Text(stringResource(id = R.string.label_no_alerts_sent))
            }
        }
    }
    if (showAddMonitor) {
        AddMonitor(
            onDismiss = { showAddMonitor = false }
        )
    }

    if (showAddProtected) {
        AddProtected(
            onDismiss = { showAddProtected = false },
            onProtectedAdded = {}
        )
    }

    if(showCancelAlert){
        CancelAlert(onDismiss = { showCancelAlert = false })
    }
}
