package pt.isec.a2023131593.AMovProjetoKotlin.ui.other

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected
import kotlin.collections.emptyList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedRules(
    monitorUid: String,
    navController: NavHostController
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    var selectedItem by remember { mutableStateOf("SafetYSec") }
    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val firestore = FirebaseFirestore.getInstance()
    var monitorName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    val allowedState = remember { mutableStateMapOf<String, Boolean>() }

    val context = LocalContext.current
    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val (hasRequiredPermissions, permissionLauncher) =
        rememberPermissionsState(permissions)

    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            listenForAlerts(context, uid, AlertType.PANIC)
            listenForAlerts(context, uid, AlertType.FALL)
            listenForAlerts(context, uid, AlertType.ACCIDENT)
            listenForAlerts(context, uid, AlertType.SPEED)
            listenForAlerts(context, uid, AlertType.GEOFENCING)
            listenForAlerts(context, uid, AlertType.INACTIVITY)
        }
    }

    val schedulesState = remember {
        mutableStateMapOf<String, SnapshotStateMap<Int, Pair<String, String>>>()
    }

    val ruleParametersState = remember {
        mutableStateMapOf<String, SnapshotStateList<String>>()
    }

    val originalSchedules = remember {
        mutableStateMapOf<String, Map<Int, Pair<String, String>>>()
    }

    val originalParameters = remember {
        mutableStateMapOf<String, List<String>>()
    }

    LaunchedEffect(monitorUid) {
        firestore.collection("User")
            .document(monitorUid)
            .get()
            .addOnSuccessListener {
                monitorName = it.getString("Nome") ?: ""
                phoneNumber = it.getString("Telemovel") ?: ""
            }
    }

    LaunchedEffect(monitorUid) {
        firestore.collection("Rule")
            .document(monitorUid)
            .get()
            .addOnSuccessListener { doc ->
                val monitorMap = doc.get(currentUserId) as? Map<*, *> ?: return@addOnSuccessListener

                schedulesState.clear()
                ruleParametersState.clear()
                originalSchedules.clear()
                originalParameters.clear()

                listOf("FALL", "ACCIDENT", "GEOFENCING", "INACTIVITY", "SPEED").forEach { rule ->
                    val ruleMap = monitorMap[rule] as? Map<*, *> ?: return@forEach
                    val allowed = ruleMap["allowed"] as? Boolean ?: false
                    allowedState[rule] = allowed

                    val scheduleDays = ruleMap["scheduleDays"] as? List<*> ?: emptyList<Any>()
                    val scheduleMap = mutableStateMapOf<Int, Pair<String, String>>()

                    var i = 0
                    while (i + 2 < scheduleDays.size) {
                        val day = (scheduleDays[i] as Number).toInt()
                        val start = scheduleDays[i + 1] as String
                        val end = scheduleDays[i + 2] as String
                        scheduleMap[day] = start to end
                        i += 3
                    }

                    val paramsFromDb = ruleMap["parameters"] as? List<String> ?: emptyList()

                    val fixedParams = when (rule) {
                        "GEOFENCING" -> listOf(
                            paramsFromDb.getOrNull(0) ?: "",
                            paramsFromDb.getOrNull(1) ?: ""
                        )
                        "SPEED" -> listOf(
                            paramsFromDb.getOrNull(0) ?: ""
                        )
                        "INACTIVITY" -> listOf(
                            paramsFromDb.getOrNull(0) ?: ""
                        )
                        "FALL", "ACCIDENT" -> emptyList()
                        else -> emptyList()
                    }


                    schedulesState[rule] = scheduleMap
                    ruleParametersState[rule] = fixedParams.toMutableStateList()

                    originalSchedules[rule] = scheduleMap.toMap()
                    originalParameters[rule] = fixedParams.toList()
                }
            }
    }

    fun saveSchedulesToFirestore() {
        val updates = mutableMapOf<String, Any>()

        schedulesState.forEach { (rule, scheduleMap) ->

            val scheduleDays = mutableListOf<Any>()
            scheduleMap.forEach { (day, times) ->
                scheduleDays.add(day)
                scheduleDays.add(times.first)
                scheduleDays.add(times.second)
            }

            val schedulesChanged =
                originalSchedules[rule] != scheduleMap.toMap()

            val parametersChanged =
                originalParameters[rule] != ruleParametersState[rule]?.toList()

            if (!schedulesChanged && !parametersChanged) return@forEach

            val basePath = "$currentUserId.$rule"

            updates["$basePath.allowed"] = false
            updates["$basePath.scheduleDays"] = scheduleDays
            updates["$basePath.parameters"] =
                ruleParametersState[rule]?.toList() ?: emptyList<String>()
        }

        if (updates.isEmpty()) return

        firestore.collection("Rule")
            .document(monitorUid)
            .update(updates)
    }

    fun parameterLabelsForRule(ruleName: String): List<String> =
        when (ruleName) {
            "GEOFENCING" -> listOf("GPS Coordinates", "Radius")
            "SPEED" -> listOf("Max Speed (km/h)")
            "INACTIVITY" -> listOf("Duration (minutes)")
            else -> emptyList()
        }


    @Composable
    fun ExpandableSchedule(
        ruleName: String,
        schedule: SnapshotStateMap<Int, Pair<String, String>>,
        parameters: SnapshotStateList<String>,
        allowed: Boolean
    ) {
        var expanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { expanded = !expanded },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(ruleName, style = MaterialTheme.typography.titleLarge)

                if (expanded) {
                    Spacer(Modifier.height(12.dp))

                    daysOfWeek.forEach { (day, dayName) ->
                        val enabled = schedule.containsKey(day)

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFEFEF))
                                .padding(8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dayName)
                                val paramsValid = areParametersValid(ruleName, parameters)

                                Switch(
                                    checked = enabled,
                                    onCheckedChange = {
                                        if (!editMode) return@Switch

                                        if (it && !paramsValid) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Please fill all required parameters first"
                                                )
                                            }
                                            return@Switch
                                        }

                                        if (!it) {
                                            schedule.remove(day)
                                        } else {
                                            schedule[day] = "08:00" to "17:00"
                                        }
                                    },
                                    enabled = editMode
                                )
                            }

                            if (enabled) {
                                val (start, end) = schedule[day]!!

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = start,
                                        onValueChange = { if (editMode) schedule[day] = it to end },
                                        enabled = editMode,
                                        label = { Text("Start") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = end,
                                        onValueChange = { if (editMode) schedule[day] = start to it },
                                        enabled = editMode,
                                        label = { Text("End") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    Spacer(Modifier.height(12.dp))

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFEFEF))
                            .padding(8.dp)
                    ) {
                        Text("Parameters", style = MaterialTheme.typography.titleMedium)

                        if (parameters.isEmpty()) {
                            Text(
                                text = "No parameters",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            val labels = parameterLabelsForRule(ruleName)

                            parameters.forEachIndexed { index, param ->
                                val displayValue =
                                    if (editMode) param
                                    else if (allowed) param
                                    else ""

                                if (!areParametersValid(ruleName, parameters) && editMode) {
                                    Text(
                                        text = "All parameters must be filled to enable schedules",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = labels.getOrNull(index) ?: "Parameter",
                                        modifier = Modifier.width(140.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    OutlinedTextField(
                                        value = displayValue,
                                        onValueChange = { if (editMode) parameters[index] = it },
                                        enabled = editMode,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
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
                onAddProtectedClick = { showAddProtected = true },
                onCancelAlertClick = { showCancelAlert = true}
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Monitorization Rules") },
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
                    requestPermissions = {
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Monitor Name", style = MaterialTheme.typography.labelLarge)
                Text(monitorName, style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(16.dp))

                Text("Phone Number", style = MaterialTheme.typography.labelLarge)
                Text(phoneNumber, style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(24.dp))

                schedulesState.forEach { (rule, schedule) ->
                    ExpandableSchedule(
                        ruleName = rule,
                        schedule = schedule,
                        parameters = ruleParametersState[rule]!!,
                        allowed = allowedState[rule] == true
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (editMode) saveSchedulesToFirestore()
                        editMode = !editMode
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editMode) "Confirm" else "Propose Changes")
                }
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

fun areParametersValid(ruleName: String, parameters: List<String>): Boolean =
    when (ruleName) {
        "GEOFENCING" -> parameters.size == 2 && parameters.all { it.isNotBlank() }
        "SPEED" -> parameters.size == 1 && parameters[0].isNotBlank()
        "INACTIVITY" -> parameters.size == 1 && parameters[0].isNotBlank()
        else -> true
    }