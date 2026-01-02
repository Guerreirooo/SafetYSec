package pt.isec.a2023131593.AMovProjetoKotlin.ui.relationships

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key

@Composable
fun AddProtected(
    onDismiss: () -> Unit,
    onProtectedAdded: () -> Unit
) {
    val codeLength = 5
    var code by remember { mutableStateOf(List(codeLength) { "" }) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var hasProcessed by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    val focusRequesters = List(codeLength) { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(code.joinToString("")) {
        val joinedCode = code.joinToString("")
        if (joinedCode.length == codeLength && !hasProcessed && currentUserId != null) {
            hasProcessed = true

            firestore.collection("OneTimePass")
                .get()
                .addOnSuccessListener { documents ->
                    var protectedUid: String? = null
                    var otpDocId: String? = null

                    for (doc in documents) {
                        val docCode = doc.getString("code")
                        val docUid = doc.id
                        if (docCode == joinedCode && docUid != currentUserId) {
                            protectedUid = docUid
                            otpDocId = doc.id
                            break
                        }
                    }

                    if (protectedUid != null) {
                        val userRel = firestore.collection("Relationship").document(currentUserId)
                        val protectedRel = firestore.collection("Relationship").document(protectedUid)

                        userRel.get().addOnSuccessListener { snap ->
                            if (snap.exists()) {
                                userRel.update("protected", FieldValue.arrayUnion(protectedUid))
                            } else {
                                userRel.set(mapOf("protected" to listOf(protectedUid)))
                            }

                            protectedRel.get().addOnSuccessListener { pSnap ->
                                if (pSnap.exists()) {
                                    protectedRel.update("monitor", FieldValue.arrayUnion(currentUserId))
                                } else {
                                    protectedRel.set(mapOf("monitor" to listOf(currentUserId)))
                                }

                                otpDocId?.let {
                                    firestore.collection("OneTimePass").document(it).delete()
                                }

                                resultText = "Protegido adicionado"
                                isSuccess = true
                                onProtectedAdded()
                                keyboardController?.hide()
                            }
                        }
                    } else {
                        resultText = "Código inválido"
                        isSuccess = false
                    }
                }
                .addOnFailureListener {
                    resultText = "Erro ao verificar código"
                    isSuccess = false
                }
        } else if (joinedCode.length < codeLength) {
            hasProcessed = false
            resultText = null
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Insira o código presente no ecrã do protegido",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        code.forEachIndexed { index, value ->
                            OutlinedTextField(
                                value = value,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                        code = code.toMutableList().also { it[index] = newValue }
                                        if (newValue.isNotEmpty() && index < codeLength - 1) {
                                            focusRequesters[index + 1].requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(56.dp)
                                    .focusRequester(focusRequesters[index])
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                                            if (code[index].isEmpty() && index > 0) {
                                                focusRequesters[index - 1].requestFocus()
                                            }
                                        }
                                        false
                                    },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontSize = 20.sp
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    resultText?.let {
                        Text(
                            text = it,
                            color = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}