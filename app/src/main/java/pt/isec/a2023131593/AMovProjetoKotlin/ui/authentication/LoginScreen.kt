package pt.isec.a2023131593.AMovProjetoKotlin.ui.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuthException
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                text = "Login",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    // 🔹 Validação antes do login
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Email e password têm de ser preenchidos"
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

                                // 🔹 Sempre mostrar "Email ou password incorretos" para erros de credenciais
                                errorMessage = when (exception?.errorCode) {
                                    "ERROR_WRONG_PASSWORD",
                                    "ERROR_USER_NOT_FOUND",
                                    "ERROR_INVALID_CREDENTIAL",
                                    "INVALID_LOGIN_CREDENTIALS" ->
                                        "Email ou password incorretos"

                                    "ERROR_INVALID_EMAIL" -> "Email inválido"
                                    "ERROR_USER_DISABLED" -> "Conta desativada"
                                    else -> "Email ou password incorretos"
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
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onRegister) {
                    Text("Create account")
                }

                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            errorMessage = "Introduz o email para recuperar a password"
                        } else {
                            isLoading = true
                            errorMessage = null

                            auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    isLoading = false

                                    if (task.isSuccessful) {
                                        errorMessage =
                                            "Email de recuperação enviado. Verifica a tua caixa de entrada."
                                    } else {
                                        val exception =
                                            task.exception as? FirebaseAuthException

                                        errorMessage = when (exception?.errorCode) {
                                            "ERROR_INVALID_EMAIL" -> "Email inválido"
                                            "ERROR_USER_NOT_FOUND" -> "Não existe conta associada a este email"
                                            else -> exception?.localizedMessage
                                        }
                                    }
                                }
                        }
                    }
                ) {
                    Text("Forget password")
                }
            }
        }
    }
}