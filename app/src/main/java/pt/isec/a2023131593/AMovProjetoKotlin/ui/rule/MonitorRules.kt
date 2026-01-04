package pt.isec.a2023131593.AMovProjetoKotlin.ui.rule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import android.Manifest
import androidx.compose.ui.res.stringResource
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.model.rememberPermissionsState
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected
import pt.isec.a2023131593.AMovProjetoKotlin.R
import pt.isec.a2023131593.AMovProjetoKotlin.ui.alert.CancelAlert
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorRules(
    monitorUid: String,
    navController: NavHostController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: return

    val permissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    val (hasRequiredPermissions, permissionLauncher) =
        rememberPermissionsState(permissions)

    val defaultTitle = stringResource(id = R.string.app_name)
    var selectedItem by remember { mutableStateOf(defaultTitle) }
    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()
    var monitorName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    val translatedDays = listOf(
        1 to stringResource(id = R.string.day_1),
        2 to stringResource(id = R.string.day_2),
        3 to stringResource(id = R.string.day_3),
        4 to stringResource(id = R.string.day_4),
        5 to stringResource(id = R.string.day_5),
        6 to stringResource(id = R.string.day_6),
        7 to stringResource(id = R.string.day_7)
    )

    var schedules by remember {
        mutableStateOf<Map<String, MutableMap<Int, Pair<String, String>>>>(
            mapOf(
                "FALL" to mutableMapOf(),
                "ACCIDENT" to mutableMapOf(),
                "GEOFENCING" to mutableMapOf(),
                "INACTIVITY" to mutableMapOf(),
                "SPEED" to mutableMapOf()
            )
        )
    }

    var ruleParameters by remember {
        mutableStateOf<Map<String, List<String>>>(
            mapOf(
                "FALL" to emptyList(),
                "ACCIDENT" to emptyList(),
                "GEOFENCING" to emptyList(),
                "INACTIVITY" to emptyList(),
                "SPEED" to emptyList()
            )
        )
    }

    LaunchedEffect(monitorUid) {
        firestore.collection("User")
            .document(monitorUid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    monitorName = document.getString("Nome") ?: ""
                    phoneNumber = document.getString("Telemovel") ?: ""
                }
            }
    }

    LaunchedEffect(monitorUid) {
        firestore.collection("Rule")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                val monitorMap = doc.get(monitorUid) as? Map<*, *> ?: return@addOnSuccessListener
                val newSchedules = mutableMapOf<String, MutableMap<Int, Pair<String, String>>>()
                val newParameters = mutableMapOf<String, List<String>>()

                listOf("FALL", "ACCIDENT", "GEOFENCING", "INACTIVITY", "SPEED").forEach { rule ->
                    val ruleMap = monitorMap[rule] as? Map<*, *> ?: return@forEach
                    val scheduleList = ruleMap["scheduleDays"] as? List<*> ?: emptyList<Any>()
                    val parsed = mutableMapOf<Int, Pair<String, String>>()
                    var i = 0
                    while (i + 2 < scheduleList.size) {
                        val day = when (val d = scheduleList[i]) {
                            is Long -> d.toInt()
                            is Int -> d
                            is Double -> d.toInt()
                            else -> null
                        }
                        val start = scheduleList[i + 1] as? String
                        val end = scheduleList[i + 2] as? String
                        if (day != null && start != null && end != null) parsed[day] = start to end
                        i += 3
                    }
                    newSchedules[rule] = parsed

                    val params = ruleMap["parameters"] as? List<String> ?: emptyList()
                    newParameters[rule] = params
                }

                schedules = newSchedules
                ruleParameters = newParameters
            }
    }

    @Composable
    fun ExpandableSchedule(
        ruleName: String,
        schedule: MutableMap<Int, Pair<String, String>>,
        parameters: List<String>
    ) {
        var expanded by remember { mutableStateOf(false) }
        val rulesRequiringParams = listOf("GEOFENCING", "SPEED", "INACTIVITY")
        val needsParameters = ruleName in rulesRequiringParams && parameters.isEmpty()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { expanded = !expanded },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ruleName, style = MaterialTheme.typography.titleLarge)
                    if (needsParameters && editMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(Necessita parâmetros)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red
                        )
                    }
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    translatedDays.forEach { (dayNumber, dayName) ->
                        val isEnabled = schedule.containsKey(dayNumber)
                        val hours = schedule[dayNumber]

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color(0xFFEFEFEF))
                                .padding(8.dp)
                        ) {
                            var toggleState by remember { mutableStateOf(isEnabled) }

                            LaunchedEffect(isEnabled) { toggleState = isEnabled }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dayName)
                                Switch(
                                    checked = toggleState,
                                    onCheckedChange = { isChecked ->
                                        if (editMode) {
                                             if (isChecked && needsParameters) {
                                             } else {
                                                toggleState = isChecked
                                                if (!isChecked) schedule.remove(dayNumber)
                                                else schedule[dayNumber] = "08:00" to "17:00"
                                            }
                                        }
                                    },
                                    enabled = editMode && !(needsParameters && !toggleState)
                                )
                            }

                            if (toggleState) {
                                val startState = remember { mutableStateOf(hours?.first ?: "08:00") }
                                val endState = remember { mutableStateOf(hours?.second ?: "17:00") }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(id = R.string.label_start_hour), style = MaterialTheme.typography.labelMedium)
                                        OutlinedTextField(
                                            value = startState.value,
                                            onValueChange = {
                                                startState.value = it
                                                schedule[dayNumber] = it to endState.value
                                            },
                                            enabled = editMode,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(id = R.string.label_end_time), style = MaterialTheme.typography.labelMedium)
                                        OutlinedTextField(
                                            value = endState.value,
                                            onValueChange = {
                                                endState.value = it
                                                schedule[dayNumber] = startState.value to it
                                            },
                                            enabled = editMode,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFEFEF))
                            .padding(8.dp)
                    ) {
                        Text(stringResource(id = R.string.label_parameters), style = MaterialTheme.typography.titleMedium)
                        if (parameters.isEmpty()) {
                            Text(
                                stringResource(id = R.string.no_parameters),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (ruleName in rulesRequiringParams) Color.Red else Color.Unspecified
                            )
                        } else {
                            parameters.forEach { param ->
                                Text(param, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    fun saveSchedulesToFirestore() {
        val monitorData = mutableMapOf<String, Any>()
        schedules.forEach { (rule, map) ->
            val scheduleDays = mutableListOf<Any>()
            map.forEach { (day, times) ->
                scheduleDays.add(day)
                scheduleDays.add(times.first)
                scheduleDays.add(times.second)
            }

            val params: List<Any> = ruleParameters[rule]?.map { it as Any } ?: emptyList()

            monitorData[rule] = mapOf(
                "allowed" to map.isNotEmpty(),
                "parameters" to params,
                "scheduleDays" to scheduleDays
            )
        }

        val dataToSave = mapOf(monitorUid to monitorData)
        firestore.collection("Rule")
            .document(currentUserId)
            .set(dataToSave)
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
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(id = R.string.title_monitor_rules)) },
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(stringResource(id = R.string.label_monitor_name), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(monitorName, style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(id = R.string.label_phone_number), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(phoneNumber, style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(32.dp))

                schedules.forEach { (ruleName, scheduleMap) ->
                    ExpandableSchedule(
                        ruleName,
                        scheduleMap,
                        parameters = ruleParameters[ruleName] ?: emptyList()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (editMode) saveSchedulesToFirestore()
                        editMode = !editMode
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (editMode)
                            stringResource(id = R.string.btn_finish_edit)
                        else
                            stringResource(id = R.string.btn_edit)
                    )
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