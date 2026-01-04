package pt.isec.a2023131593.AMovProjetoKotlin.ui.other

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
import pt.isec.a2023131593.AMovProjetoKotlin.model.AlertType
import pt.isec.a2023131593.AMovProjetoKotlin.model.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.model.listenForAlerts
import pt.isec.a2023131593.AMovProjetoKotlin.model.rememberPermissionsState
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.BottomNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.LeftNavBar
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddMonitor
import pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships.AddProtected
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    navController: NavHostController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedItem by remember { mutableStateOf("SafetYSec") }
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
                    errorMessage = "Error loading data: ${e.localizedMessage}"
                    isLoading = false
                }
        } else {
            errorMessage = "User not logged"
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
                    title = { Text("Profile") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
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
                                label = { Text("Email") },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Change Password",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        isLoading = true
                                        errorMessage = null
                                        auth.sendPasswordResetEmail(email)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                errorMessage = if (task.isSuccessful) {
                                                    "Recovery email sent. Please check your inbox."
                                                } else {
                                                    val exception = task.exception as? FirebaseAuthException
                                                    when (exception?.errorCode) {
                                                        "ERROR_INVALID_EMAIL" -> "Invalid Email"
                                                        "ERROR_USER_NOT_FOUND" -> "There is no account associated with this email address."
                                                        else -> exception?.localizedMessage
                                                    }
                                                }
                                            }
                                    }
                                    .padding(vertical = 4.dp)
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = if (errorMessage!!.contains("sent"))
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
                                label = { Text("Name") },
                                enabled = isEditing,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = codigoAlerta,
                                onValueChange = { codigoAlerta = it },
                                label = { Text("Alert Code") },
                                enabled = isEditing,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = telemovel,
                                onValueChange = { telemovel = it },
                                label = { Text("Phone") },
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
                                                    errorMessage =
                                                        "Error saving: ${e.localizedMessage}"
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
                                Text(if (isEditing) "Save" else "Edit")
                            }

                            Button(
                                onClick = {
                                    auth.signOut()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 16.dp)
                            ) {
                                Text("Logout")
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