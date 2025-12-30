package pt.isec.a2023131593.AMovProjetoKotlin.ui.Navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
    onItemSelected: (String) -> Unit
) {
    val menuItems = listOf(
        "Associar Monitor",
        "Adicionar Protegido",
        "Propostas de Monitorização",
        "Cancelar Alerta"
    )

    ModalDrawerSheet(
        modifier = Modifier.width(250.dp)
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

                        // Navegação (ajuste conforme cada rota)
                        when (item) {
                            "Associar Monitor" -> navController.navigate(Routes.HOME)
                            "Adicionar Protegido" -> navController.navigate(Routes.HOME)
                            "Propostas de Monitorização" -> navController.navigate(Routes.HOME)
                            "Cancelar Alerta" -> navController.navigate(Routes.HOME)
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when(item) {
                    "Associar Monitor" -> Icons.Default.Person
                    "Adicionar Protegido" -> Icons.Default.PersonAdd
                    "Propostas de Monitorização" -> Icons.Default.Description
                    "Cancelar Alerta" -> Icons.Default.Cancel
                    else -> null
                }

                if (icon != null) {
                    Icon(
                        imageVector = icon,
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