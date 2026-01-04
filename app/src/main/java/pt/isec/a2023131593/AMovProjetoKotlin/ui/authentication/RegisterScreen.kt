package pt.isec.a2023131593.AMovProjetoKotlin.ui.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import pt.isec.a2023131593.AMovProjetoKotlin.R

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
    var telemovel by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessageId by remember { mutableStateOf<Int?>(null) }
    var externalError by remember { mutableStateOf<String?>(null) }

    val firestore = FirebaseFirestore.getInstance()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.desc_back)
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
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

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

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text(stringResource(id = R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = codigoAlerta,
                    onValueChange = { codigoAlerta = it },
                    label = { Text(stringResource(id = R.string.label_alert_code)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = telemovel,
                    onValueChange = { telemovel = it },
                    label = { Text(stringResource(id = R.string.label_phone)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessageId != null) {
                    Text(
                        text = externalError!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (nome.isBlank() || codigoAlerta.isBlank() || telemovel.isBlank() || email.isBlank() || password.isBlank()) {
                            errorMessageId = R.string.error_all_fields
                            return@Button
                        }

                        isLoading = true
                        errorMessageId = null
                        externalError = null

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
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
                                        .addOnSuccessListener { onRegisterSuccess() }
                                        .addOnFailureListener { e ->
                                            externalError = e.localizedMessage
                                        }
                                } else {
                                    val exception = task.exception as? FirebaseAuthException
                                    when (exception?.errorCode) {
                                        "ERROR_EMAIL_ALREADY_IN_USE" -> errorMessageId = R.string.error_email_in_use
                                        "ERROR_INVALID_EMAIL" -> errorMessageId = R.string.error_invalid_email
                                        "ERROR_WEAK_PASSWORD" -> errorMessageId = R.string.error_weak_password
                                        else -> externalError = exception?.localizedMessage
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
                        Text(stringResource(id = R.string.btn_register))
                    }
                }
            }
        }
    }
}