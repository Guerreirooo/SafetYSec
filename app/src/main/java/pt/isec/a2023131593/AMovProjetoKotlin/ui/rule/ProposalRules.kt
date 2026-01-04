package pt.isec.a2023131593.AMovProjetoKotlin.ui.rule

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.listenForAlerts
import pt.isec.a2023131593.AMovProjetoKotlin.model.rememberPermissionsState
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected
import pt.isec.a2023131593.AMovProjetoKotlin.ui.alert.CancelAlert
import pt.isec.a2023131593.AMovProjetoKotlin.ui.alert.translateAlertType
import pt.isec.a2023131593.AMovProjetoKotlin.R
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalRules(
    navController: NavHostController
) {
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }

    val daysOfWeek = listOf(
        1 to stringResource(id = R.string.day_1),
        2 to stringResource(id = R.string.day_2),
        3 to stringResource(id = R.string.day_3),
        4 to stringResource(id = R.string.day_4),
        5 to stringResource(id = R.string.day_5),
        6 to stringResource(id = R.string.day_6),
        7 to stringResource(id = R.string.day_7)
    ).toMap()

    var proposals by remember {
        mutableStateOf<Map<String, Map<String, Map<String, Any>>>>(emptyMap())
    }

    var monitorNames by remember {
        mutableStateOf<Map<String, String>>(emptyMap())
    }

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

    LaunchedEffect(Unit) {
        firestore.collection("Rule")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                val result = mutableMapOf<String, Map<String, Map<String, Any>>>()
                val namesMap = mutableMapOf<String, String>()

                doc.data?.forEach { (monitorUid, rulesAny) ->
                    val rulesMap = rulesAny as? Map<*, *> ?: return@forEach
                    val filteredRules = mutableMapOf<String, Map<String, Any>>()

                    rulesMap.forEach { (ruleName, ruleDataAny) ->
                        val ruleData = ruleDataAny as? Map<*, *> ?: return@forEach
                        val allowed = ruleData["allowed"] as? Boolean ?: true
                        val parameters = ruleData["parameters"] as? List<*> ?: emptyList<Any>()
                        val scheduleDays = ruleData["scheduleDays"] as? List<*> ?: emptyList<Any>()

                        if (!allowed && (parameters.isNotEmpty() || scheduleDays.isNotEmpty())) {
                            filteredRules[ruleName.toString()] =
                                ruleData.mapKeys { it.key.toString() } as Map<String, Any>
                        }
                    }

                    if (filteredRules.isNotEmpty()) {
                        result[monitorUid] = filteredRules
                        firestore.collection("User")
                            .document(monitorUid)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("Nome") ?: monitorUid
                                namesMap[monitorUid] = name
                                monitorNames = namesMap.toMap()
                            }
                    }
                }
                proposals = result
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LeftNavBar(
                navController = navController,
                drawerState = drawerState,
                scope = scope,
                selectedItem = stringResource(id = R.string.nav_monitoring_proposals),
                onItemSelected = {},
                onAddMonitorClick = { showAddMonitor = true },
                onAddProtectedClick = { showAddProtected = true },
                onCancelAlertClick = { showCancelAlert = true }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(id = R.string.title_proposal_rules)) },
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
                    selectedRoute = Routes.DASHBOARD,
                    hasRequiredPermissions = hasRequiredPermissions,
                    requestPermissions = {
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (proposals.isEmpty()) {
                    Text(stringResource(id = R.string.msg_no_proposals))
                    return@Column
                }

                proposals.forEach { (monitorUid, rules) ->
                    val monitorName = monitorNames[monitorUid] ?: monitorUid

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(id = R.string.label_monitor_prefix, monitorName),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            rules.forEach { (ruleName, ruleData) ->
                                val translatedRuleName = translateAlertType(ruleName)

                                val paramToggles = remember(monitorUid + ruleName) {
                                    mutableStateMapOf<String, Boolean>().apply {
                                        val parameters = ruleData["parameters"] as? List<*> ?: emptyList<Any>()
                                        parameters.forEach { param -> this[param.toString()] = false }
                                    }
                                }

                                val scheduleToggles = remember(monitorUid + ruleName) {
                                    mutableStateMapOf<Int, Triple<Boolean, String, String>>().apply {
                                        val scheduleDays = ruleData["scheduleDays"] as? List<*> ?: emptyList<Any>()
                                        var i = 0
                                        while (i + 2 < scheduleDays.size) {
                                            val dayNumber = (scheduleDays[i] as? Number)?.toInt() ?: -1
                                            val start = scheduleDays[i + 1].toString()
                                            val end = scheduleDays[i + 2].toString()
                                            if (dayNumber != -1) this[dayNumber] = Triple(false, start, end)
                                            i += 3
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEFEFEF))
                                        .padding(8.dp)
                                ) {
                                    Text(translatedRuleName, style = MaterialTheme.typography.titleSmall)

                                    if (paramToggles.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(stringResource(id = R.string.label_parameters),
                                                style = MaterialTheme.typography.labelLarge)

                                        val parameterList = paramToggles.keys.toList()
                                        parameterList.forEachIndexed { index, param ->
                                            val checked = paramToggles[param] ?: false

                                            val parameterLabel = when (ruleName.uppercase()) {
                                                "SPEED" -> stringResource(id = R.string.label_speed_param)
                                                "INACTIVITY" -> stringResource(id = R.string.label_inactivity_param)
                                                "GEOFENCING" -> {
                                                    if (index == 0) stringResource(id = R.string.label_geofencing_coords)
                                                    else stringResource(id = R.string.label_geofencing_radius)
                                                }
                                                else -> ""
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { paramToggles[param] = it }
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(text = "$parameterLabel$param")
                                            }
                                        }
                                    }

                                    if (scheduleToggles.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(stringResource(id = R.string.label_proposed_schedule))
                                        scheduleToggles.forEach { (dayNumber, triple) ->
                                            val (checked, start, end) = triple
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = {
                                                        scheduleToggles[dayNumber] = Triple(it, start, end)
                                                    }
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                val dayName = daysOfWeek[dayNumber] ?: "Unknown"
                                                Text(stringResource(id = R.string.schedule_format, dayName, start, end))
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val selectedParams = paramToggles.filter { it.value }.keys.toList()
                                                val selectedSchedule = scheduleToggles.filter { it.value.first }.flatMap {
                                                    listOf(it.key, it.value.second, it.value.third)
                                                }

                                                firestore.collection("Rule")
                                                    .document(currentUserId)
                                                    .get()
                                                    .addOnSuccessListener { doc ->
                                                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                                                        val existing = data[monitorUid] as? MutableMap<String, Any> ?: mutableMapOf()

                                                        val ruleMap = mutableMapOf<String, Any>()

                                                        if (selectedParams.isNotEmpty() || selectedSchedule.isNotEmpty()) {
                                                            ruleMap["allowed"] = true
                                                            ruleMap["parameters"] = selectedParams
                                                            ruleMap["scheduleDays"] = selectedSchedule
                                                        } else {
                                                            ruleMap["allowed"] = false
                                                            ruleMap["parameters"] = emptyList<Any>()
                                                            ruleMap["scheduleDays"] = emptyList<Any>()
                                                        }

                                                        existing[ruleName] = ruleMap
                                                        data[monitorUid] = existing

                                                        firestore.collection("Rule")
                                                            .document(currentUserId)
                                                            .set(data)
                                                            .addOnSuccessListener {
                                                                navController.navigate(Routes.PROPOSAL_RULES) {
                                                                    popUpTo(Routes.PROPOSAL_RULES) { inclusive = true }
                                                                }
                                                            }
                                                    }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784))
                                        ) {
                                            Text(stringResource(id = R.string.btn_accept))
                                        }

                                        Button(
                                            onClick = {
                                                val ruleMap = mutableMapOf<String, Any>(
                                                    "allowed" to false,
                                                    "parameters" to emptyList<Any>(),
                                                    "scheduleDays" to emptyList<Any>()
                                                )

                                                firestore.collection("Rule")
                                                    .document(currentUserId)
                                                    .get()
                                                    .addOnSuccessListener { doc ->
                                                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                                                        val existing = data[monitorUid] as? MutableMap<String, Any> ?: mutableMapOf()

                                                        existing[ruleName] = ruleMap
                                                        data[monitorUid] = existing

                                                        firestore.collection("Rule")
                                                            .document(currentUserId)
                                                            .set(data)
                                                            .addOnSuccessListener {
                                                                navController.navigate(Routes.PROPOSAL_RULES) {
                                                                    popUpTo(Routes.PROPOSAL_RULES) { inclusive = true }
                                                                }
                                                            }
                                                    }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                                        ) {
                                            Text(stringResource(id = R.string.btn_reject), color = Color.White)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
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
