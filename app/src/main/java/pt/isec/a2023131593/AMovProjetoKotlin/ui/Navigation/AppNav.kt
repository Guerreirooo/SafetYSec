package pt.isec.a2023131593.AMovProjetoKotlin.ui.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import pt.isec.a2023131593.AMovProjetoKotlin.model.Routes
import pt.isec.a2023131593.AMovProjetoKotlin.ui.authentication.LoginScreen
import pt.isec.a2023131593.AMovProjetoKotlin.ui.authentication.RegisterScreen
import pt.isec.a2023131593.AMovProjetoKotlin.ui.home.HomeScreen
import pt.isec.a2023131593.AMovProjetoKotlin.ui.other.ProfileScreen

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
                onLoginSuccess = { navController.navigate(Routes.HOME) }
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

        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                auth = auth,
                firestore = FirebaseFirestore.getInstance(),
                navController = navController
            )
        }
    }
}