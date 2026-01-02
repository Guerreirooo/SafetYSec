package pt.isec.a2023131593.AMovProjetoKotlin

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import pt.isec.a2023131593.AMovProjetoKotlin.ui.navigation.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        val auth = FirebaseAuth.getInstance()

        setContent {
            App(auth)
        }
    }
}

@Composable
fun App(auth: FirebaseAuth) {
    val navController = rememberNavController()
    AppNav(navController = navController, auth = auth)
}