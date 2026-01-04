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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.R

private enum class MenuDestinations {
    ASSOCIATE, ADD_PROTECTED, PROPOSALS, HISTORY, CANCEL
}
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
        MenuDestinations.ASSOCIATE to R.string.menu_associate_monitor,
        MenuDestinations.ADD_PROTECTED to R.string.menu_add_protected,
        MenuDestinations.PROPOSALS to R.string.menu_monitoring_proposals,
        MenuDestinations.HISTORY to R.string.menu_alerts_history,
        MenuDestinations.CANCEL to R.string.menu_cancel_alert
    )

    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = Modifier.width(250.dp).verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        menuItems.forEach { (destination, stringId) ->
            val label = stringResource(id = stringId)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemSelected(label)
                        scope.launch { drawerState.close() }

                        when (destination) {
                            MenuDestinations.ASSOCIATE -> onAddMonitorClick()
                            MenuDestinations.ADD_PROTECTED -> onAddProtectedClick()
                            MenuDestinations.PROPOSALS -> navController.navigate(Routes.PROPOSAL_RULES)
                            MenuDestinations.HISTORY -> navController.navigate(Routes.HISTORY)
                            MenuDestinations.CANCEL -> onCancelAlertClick()
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (destination) {
                    MenuDestinations.ASSOCIATE -> Icons.Default.PersonAdd
                    MenuDestinations.ADD_PROTECTED -> Icons.Filled.Add
                    MenuDestinations.PROPOSALS -> Icons.Default.Description
                    MenuDestinations.HISTORY -> Icons.Filled.Schedule
                    MenuDestinations.CANCEL -> Icons.Default.Cancel
                }

                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
