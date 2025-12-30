package pt.isec.a2023131593.AMovProjetoKotlin.ui.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    auth: FirebaseAuth,
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var codigoAlerta by remember { mutableStateOf("") }
    var telemovel by remember { mutableStateOf("") } // NOVO CAMPO
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val firestore = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Register",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = codigoAlerta,
                    onValueChange = { codigoAlerta = it },
                    label = { Text("Código de Alerta") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = telemovel,
                    onValueChange = { telemovel = it },
                    label = { Text("Telemóvel") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (nome.isBlank() || codigoAlerta.isBlank() || telemovel.isBlank() || email.isBlank() || password.isBlank()) {
                            errorMessage = "Todos os campos têm de ser preenchidos"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid ?: ""
                                    val userMap = hashMapOf(
                                        "Nome" to nome,
                                        "CodigoAlerta" to codigoAlerta,
                                        "Telemovel" to telemovel,
                                    )

                                    firestore.collection("User")
                                        .document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onRegisterSuccess()
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            errorMessage = "Erro ao salvar dados adicionais: ${e.localizedMessage}"
                                        }
                                } else {
                                    isLoading = false
                                    val exception = task.exception as? FirebaseAuthException
                                    errorMessage = when (exception?.errorCode) {
                                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este email já está registado"
                                        "ERROR_INVALID_EMAIL" -> "Email inválido"
                                        "ERROR_WEAK_PASSWORD" -> "Password demasiado fraca (mín. 6 caracteres)"
                                        else -> exception?.localizedMessage
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
                        Text("Registar")
                    }
                }
            }
        }
    }
}
