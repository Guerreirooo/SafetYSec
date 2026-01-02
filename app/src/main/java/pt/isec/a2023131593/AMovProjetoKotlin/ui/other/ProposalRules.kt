package pt.isec.a2023131593.AMovProjetoKotlin.ui.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.model.Routes

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

    var proposals by remember {
        mutableStateOf<Map<String, Map<String, Map<String, Any>>>>(emptyMap())
    }

    var monitorNames by remember {
        mutableStateOf<Map<String, String>>(emptyMap())
    }

    val daysOfWeek = mapOf(
        1 to "Sunday",
        2 to "Monday",
        3 to "Tuesday",
        4 to "Wednesday",
        5 to "Thursday",
        6 to "Friday",
        7 to "Saturday"
    )

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
                selectedItem = "Propostas de Monitorização",
                onItemSelected = {},
                onAddMonitorClick = {},
                onAddProtectedClick = {}
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Proposed Rules") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(navController = navController, selectedRoute = Routes.PROPOSAL_RULES)
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (proposals.isEmpty()) {
                    Text("No proposed changes")
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
                                text = "Monitor: $monitorName",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            rules.forEach { (ruleName, ruleData) ->

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
                                    Text(ruleName, style = MaterialTheme.typography.titleSmall)

                                    if (paramToggles.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Parameters:")
                                        paramToggles.forEach { (param, checked) ->
                                            Row(
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .background(Color.Transparent)
                                                    .padding(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { paramToggles[param] = it }
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(param)
                                            }
                                        }
                                    }

                                    if (scheduleToggles.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Proposed Schedule:")
                                        scheduleToggles.forEach { (dayNumber, triple) ->
                                            var (checked, start, end) = triple
                                            Row(
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .background(Color.Transparent)
                                                    .padding(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = {
                                                        scheduleToggles[dayNumber] = Triple(it, start, end)
                                                    }
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                val dayName = daysOfWeek[dayNumber] ?: "Unknown"
                                                Text("$dayName: $start - $end")
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val buttonModifier = Modifier
                                            .weight(1f)

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
                                                        firestore.collection("Rule").document(currentUserId).set(data)
                                                            .addOnSuccessListener {
                                                                navController.navigate(Routes.PROPOSAL_RULES) {
                                                                    popUpTo(Routes.PROPOSAL_RULES) { inclusive = true }
                                                                }
                                                            }
                                                    }
                                            },
                                            modifier = buttonModifier
                                        ) {
                                            Text("Accept")
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
                                                        firestore.collection("Rule").document(currentUserId).set(data)
                                                            .addOnSuccessListener {
                                                                navController.navigate(Routes.PROPOSAL_RULES) {
                                                                    popUpTo(Routes.PROPOSAL_RULES) { inclusive = true }
                                                                }
                                                            }
                                                    }
                                            },
                                            modifier = buttonModifier,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            Text("Reject", color = Color.White)
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
}
