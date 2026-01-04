package pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pt.isec.a2023131593.AMovProjetoKotlin.model.enums.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.ui.authentication.LoginScreen
import pt.isec.a2023131593.AMovProjetoKotlin.ui.authentication.RegisterScreen
import pt.isec.a2023131593.AMovProjetoKotlin.ui.home.HomeScreen
import pt.isec.a2023131593.AMovProjetoKotlin.ui.alert.AlertHistory
import pt.isec.a2023131593.AMovProjetoKotlin.ui.alert.LastAlert
import pt.isec.a2023131593.AMovProjetoKotlin.ui.rule.MonitorRules
import pt.isec.a2023131593.AMovProjetoKotlin.ui.profile.ProfileScreen
import pt.isec.a2023131593.AMovProjetoKotlin.ui.rule.ProposalRules
import pt.isec.a2023131593.AMovProjetoKotlin.ui.alert.ProtectedInfo
import pt.isec.a2023131593.AMovProjetoKotlin.ui.rule.ProtectedRules

@Composable
fun AppNav(navController: NavHostController, auth: FirebaseAuth) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {

        composable(Routes.LOGIN) {
            LoginScreen(
                auth = auth,
                onRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = { navController.navigate(Routes.DASHBOARD) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                auth = auth,
                onRegisterSuccess = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.navigate(Routes.LOGIN)
                }
            )
        }

        composable(Routes.DASHBOARD) {
            HomeScreen(navController = navController)
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                auth = auth,
                firestore = FirebaseFirestore.getInstance(),
                navController = navController
            )
        }

        composable(
            route = "${Routes.MONITOR_RULES}/{monitorUid}",
            arguments = listOf(
                navArgument("monitorUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val monitorUid =
                backStackEntry.arguments?.getString("monitorUid")!!

            MonitorRules(
                monitorUid = monitorUid,
                navController = navController
            )
        }

        composable(
            route = "${Routes.PROTECTED_RULES}/{protectedUid}",
            arguments = listOf(
                navArgument("protectedUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val protectedUid =
                backStackEntry.arguments?.getString("protectedUid")!!

            ProtectedRules(
                monitorUid = protectedUid,
                navController = navController
            )
        }

        composable(Routes.PROPOSAL_RULES) {
            ProposalRules(navController = navController)
        }

        composable(Routes.HISTORY) {
            AlertHistory(navController = navController)
        }

        composable(
            route = "${Routes.INFO}/{protectedUid}",
            arguments = listOf(
                navArgument("protectedUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val protectedUid =
                backStackEntry.arguments?.getString("protectedUid")!!

            ProtectedInfo(
                protectedUid = protectedUid,
                navController = navController,
            )
        }

        composable(
            route = "${Routes.LAST_ALERT}/{protectedUid}",
            arguments = listOf(navArgument("protectedUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val protectedUid = backStackEntry.arguments?.getString("protectedUid") ?: ""
            LastAlert(protectedUid = protectedUid, navController = navController)
        }
    }
}