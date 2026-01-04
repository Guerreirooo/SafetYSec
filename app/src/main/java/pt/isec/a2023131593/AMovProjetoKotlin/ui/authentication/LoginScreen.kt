package pt.isec.a2023131593.AMovProjetoKotlin.ui.authentication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuthException
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import pt.isec.a2023131593.AMovProjetoKotlin.R
@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.label_email)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.label_password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = stringResource(id = errorMessage!!),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = R.string.error_empty_fields
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                val exception = task.exception as? FirebaseAuthException
                                errorMessage = when (exception?.errorCode) {
                                    "ERROR_WRONG_PASSWORD",
                                    "ERROR_USER_NOT_FOUND",
                                    "ERROR_INVALID_CREDENTIAL",
                                    "INVALID_LOGIN_CREDENTIALS" ->
                                        R.string.error_invalid_credentials

                                    "ERROR_INVALID_EMAIL" -> R.string.error_invalid_email
                                    "ERROR_USER_DISABLED" -> R.string.error_user_disabled
                                    else -> R.string.error_invalid_credentials
                                }
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(id = R.string.btn_login))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onRegister) {
                    Text(stringResource(id = R.string.btn_create_account))
                }

                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            errorMessage = R.string.error_reset_email_empty
                        } else {
                            isLoading = true
                            errorMessage = null

                            auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        errorMessage = R.string.info_reset_sent
                                    } else {
                                        val exception = task.exception as? FirebaseAuthException
                                        errorMessage = when (exception?.errorCode) {
                                            "ERROR_INVALID_EMAIL" -> R.string.error_invalid_email
                                            "ERROR_USER_NOT_FOUND" -> R.string.error_user_not_found
                                            else -> R.string.error_invalid_credentials
                                        }
                                    }
                                }
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.btn_forget_password))
                }
            }
        }
    }
}