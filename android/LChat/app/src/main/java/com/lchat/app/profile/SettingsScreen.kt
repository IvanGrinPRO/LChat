package com.lchat.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lchat.app.ui.components.NeumorphicTextField
import com.lchat.app.ui.neumorphism.neumorphic
import com.lchat.app.ui.theme.NeuAccent
import com.lchat.app.ui.theme.NeuSurface
import com.lchat.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val changePasswordState by viewModel.changePasswordState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmError by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.Deleted) {
            onAccountDeleted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .neumorphic(shape = CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = NeuAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Cambiar contraseña
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(shape = RoundedCornerShape(16.dp))
                .clickable {
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    confirmError = ""
                    viewModel.resetChangePasswordState()
                    showChangePasswordDialog = true
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NeuAccent,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Cambiar contraseña",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeuAccent
                )
                Text(
                    text = "Actualiza tu contraseña de acceso",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Eliminar cuenta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(shape = RoundedCornerShape(16.dp))
                .clickable(enabled = uiState !is SettingsUiState.Loading) {
                    showDeleteDialog = true
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState is SettingsUiState.Loading) {
                CircularProgressIndicator(
                    color = NeuAccent,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = NeuAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Eliminar cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeuAccent
                )
                Text(
                    text = "Esta accion no se puede deshacer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        if (uiState is SettingsUiState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = (uiState as SettingsUiState.Error).message,
                style = MaterialTheme.typography.bodyMedium,
                color = NeuAccent
            )
        }
    }

    // Dialogo cambiar contraseña
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePasswordDialog = false
                viewModel.resetChangePasswordState()
            },
            title = { Text("Cambiar contrasena") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (changePasswordState is ChangePasswordState.Success) {
                        Text(
                            text = "Contrasena actualizada correctamente!",
                            color = NeuAccent,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        NeumorphicTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            placeholder = "Contrasena actual",
                            isPassword = true
                        )
                        NeumorphicTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            placeholder = "Nueva contrasena",
                            isPassword = true
                        )
                        NeumorphicTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmError = ""
                            },
                            placeholder = "Confirmar nueva contrasena",
                            isPassword = true
                        )
                        val errorMsg = when {
                            confirmError.isNotEmpty() -> confirmError
                            changePasswordState is ChangePasswordState.Error ->
                                (changePasswordState as ChangePasswordState.Error).message
                            else -> null
                        }
                        if (errorMsg != null) {
                            Text(
                                text = errorMsg,
                                color = NeuAccent,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (changePasswordState !is ChangePasswordState.Success) {
                    TextButton(
                        onClick = {
                            if (newPassword != confirmPassword) {
                                confirmError = "Las contrasenas no coinciden"
                                return@TextButton
                            }
                            confirmError = ""
                            viewModel.changePassword(currentPassword, newPassword)
                        },
                        enabled = changePasswordState !is ChangePasswordState.Loading
                    ) {
                        if (changePasswordState is ChangePasswordState.Loading) {
                            CircularProgressIndicator(
                                color = NeuAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Guardar", color = NeuAccent)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePasswordDialog = false
                    viewModel.resetChangePasswordState()
                }) {
                    Text(if (changePasswordState is ChangePasswordState.Success) "Cerrar" else "Cancelar")
                }
            },
            containerColor = NeuSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = TextSecondary
        )
    }

    // Dialogo eliminar cuenta
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar cuenta") },
            text = { Text("Se eliminara tu cuenta permanentemente. Los chats existentes mostraran 'Deleted Account'.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount()
                }) {
                    Text("Eliminar", color = NeuAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = NeuSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = TextSecondary
        )
    }
}