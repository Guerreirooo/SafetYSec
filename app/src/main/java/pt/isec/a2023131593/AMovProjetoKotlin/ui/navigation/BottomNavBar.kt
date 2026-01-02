package pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import createPanicAlert
import pt.isec.a2023131593.AMovProjetoKotlin.model.Routes

@Composable
fun BottomNavBar(
    navController: NavHostController,
    selectedRoute: String,
) {
    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            val requestPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(
                        context,
                        "Permissão de notificações negada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            IconButton(
                onClick = { navController.navigate(Routes.DASHBOARD) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = "Dashboard",
                    tint = if (selectedRoute == Routes.DASHBOARD)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val localizationNow = GeoPoint(0.0, 0.0)

                    currentUserId?.let { uid ->
                        createPanicAlert(context, uid, localizationNow)
                    }
                },
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.size(120.dp)
            ) {
                Text(
                    text = "PANIC !",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            IconButton(
                onClick = { navController.navigate(Routes.PROFILE) }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = if (selectedRoute == Routes.PROFILE)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}