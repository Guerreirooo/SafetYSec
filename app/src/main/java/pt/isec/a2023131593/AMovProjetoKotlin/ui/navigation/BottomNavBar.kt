package pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.AlertType
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.createAlert
import pt.isec.a2023131593.AMovProjetoKotlin.model.alerts.getCurrentLocation
import pt.isec.a2023131593.AMovProjetoKotlin.R

@Composable
fun BottomNavBar(
    navController: NavHostController,
    selectedRoute: String,
    hasRequiredPermissions: Boolean,
    requestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val permissionErrorMsg = stringResource(id = R.string.error_permissions)
    val locationErrorMsg = stringResource(id = R.string.error_location)

    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            IconButton(
                onClick = { navController.navigate(Routes.DASHBOARD) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(id = R.string.desc_dashboard),
                    tint = if (selectedRoute == Routes.DASHBOARD)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    if (!hasRequiredPermissions) {
                        requestPermissions()
                        Toast.makeText(
                            context,
                            permissionErrorMsg,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    getCurrentLocation(context) { geoPoint ->
                        if (geoPoint != null) {
                            currentUserId?.let { uid ->
                                createAlert(
                                    context, uid, geoPoint, AlertType.PANIC
                                )
                            }
                        } else {
                            Toast.makeText(
                                context,
                                locationErrorMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.size(120.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.btn_panic),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            IconButton(
                onClick = { navController.navigate(Routes.PROFILE) }
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = stringResource(id = R.string.desc_profile),
                    tint = if (selectedRoute == Routes.PROFILE)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
