package pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.isec.a2023131593.AMovProjetoKotlin.model.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeftNavBar(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    onAddMonitorClick: () -> Unit,
    onAddProtectedClick: () -> Unit,
    onCancelAlertClick: () -> Unit
) {
    val menuItems = listOf(
        "Associate Monitor",
        "Add Protected",
        "Monitoring Proposals",
        "Alerts History",
        "Cancel Alert"
    )

    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = Modifier.width(250.dp).verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SafetYSec",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        menuItems.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemSelected(item)
                        scope.launch { drawerState.close() }

                        when (item) {
                            "Associate Monitor" -> onAddMonitorClick()
                            "Add Protected" -> onAddProtectedClick()
                            "Monitoring Proposals" -> navController.navigate(Routes.PROPOSAL_RULES)
                            "Alerts History" -> navController.navigate(Routes.HISTORY)
                            "Cancel Alert" -> onCancelAlertClick()
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (item) {
                    "Associate Monitor" -> Icons.Default.PersonAdd
                    "Add Protected" -> Icons.Filled.Add
                    "Monitoring Proposals" -> Icons.Default.Description
                    "Alerts History" -> Icons.Filled.Schedule
                    "Cancel Alert" -> Icons.Default.Cancel
                    else -> null
                }

                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = item,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
