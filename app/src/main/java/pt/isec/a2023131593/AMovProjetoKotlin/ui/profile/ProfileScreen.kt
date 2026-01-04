package pt.isec.a2023131593.AMovProjetoKotlin.ui.profile

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
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
import pt.isec.a2023131593.AMovProjetoKotlin.R
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.checkAllRules
import pt.isec.a2023131593.AMovProjetoKotlin.ui.alert.CancelAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    navController: NavHostController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val defaultTitle = stringResource(id = R.string.app_name)
    var selectedItem by remember { mutableStateOf(defaultTitle) }
    var showAddMonitor by remember { mutableStateOf(false) }
    var showAddProtected by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }
    var monitors by remember { mutableStateOf<List<String>>(emptyList()) }
    var protected by remember { mutableStateOf<List<String>>(emptyList()) }

    val userId = auth.currentUser?.uid
    val email = auth.currentUser?.email ?: ""
    val context = LocalContext.current

    var nome by remember { mutableStateOf("") }
    var codigoAlerta by remember { mutableStateOf("") }
    var telemovel by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessageId by remember { mutableStateOf<Int?>(null) }
    var externalError by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }

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
        if (userId != null) {
            firestore.collection("User")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nome = document.getString("Nome") ?: ""
                        codigoAlerta = document.getString("CodigoAlerta") ?: ""
                        telemovel = document.getString("Telemovel") ?: ""
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    externalError = e.localizedMessage
                    isLoading = false
                }
        } else {
            errorMessageId = R.string.error_user_not_logged
            isLoading = false
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
                    title = { Text(stringResource(id = R.string.title_profile)) },
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
                    selectedRoute = Routes.PROFILE,
                    hasRequiredPermissions = hasRequiredPermissions,
                    requestPermissions = { permissionLauncher.launch(permissions.toTypedArray()) }
                )
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = {},
                                label = { Text(stringResource(id = R.string.label_email)) },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(id = R.string.label_change_password),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        isLoading = true
                                        errorMessageId = null
                                        externalError = null
                                        auth.sendPasswordResetEmail(email)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    errorMessageId = R.string.info_recovery_sent
                                                } else {
                                                    val exception = task.exception as? FirebaseAuthException
                                                    when (exception?.errorCode) {
                                                        "ERROR_INVALID_EMAIL" -> errorMessageId = R.string.error_invalid_email
                                                        "ERROR_USER_NOT_FOUND" -> errorMessageId = R.string.error_user_not_found
                                                        else -> externalError = exception?.localizedMessage
                                                    }
                                                }
                                            }
                                    }
                                    .padding(vertical = 4.dp)
                            )

                            val displayError = when {
                                errorMessageId != null -> stringResource(id = errorMessageId!!)
                                externalError != null -> externalError
                                else -> null
                            }

                            if (displayError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = displayError,
                                    color = if (errorMessageId == R.string.info_recovery_sent)
                                        MaterialTheme.colorScheme.secondary
                                    else
                                        MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = nome,
                                onValueChange = { nome = it },
                                label = { Text(stringResource(id = R.string.label_name)) },
                                enabled = isEditing,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = codigoAlerta,
                                onValueChange = { codigoAlerta = it },
                                label = { Text(stringResource(id = R.string.label_alert_code)) },
                                enabled = isEditing,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = telemovel,
                                onValueChange = { telemovel = it },
                                label = { Text(stringResource(id = R.string.label_phone)) },
                                enabled = isEditing,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (isEditing) {
                                        userId?.let { uid ->
                                            val updatedData = hashMapOf(
                                                "Nome" to nome,
                                                "CodigoAlerta" to codigoAlerta,
                                                "Telemovel" to telemovel
                                            )
                                            firestore.collection("User")
                                                .document(uid)
                                                .update(updatedData as Map<String, Any>)
                                                .addOnSuccessListener { isEditing = false }
                                                .addOnFailureListener { e ->
                                                    externalError = e.localizedMessage
                                                }
                                        }
                                    } else {
                                        isEditing = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (isEditing)
                                        stringResource(id = R.string.btn_save)
                                    else
                                        stringResource(id = R.string.btn_edit)
                                )
                            }

                            Button(
                                onClick = {
                                    checkAllRules.stopMonitoring(context)
                                    auth.signOut()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 16.dp)
                            ) {
                                Text(text = stringResource(id = R.string.btn_logout))
                            }
                        }
                    }
                }
            }
        )
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