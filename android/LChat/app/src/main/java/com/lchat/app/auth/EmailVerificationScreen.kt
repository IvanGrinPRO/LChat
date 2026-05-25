package com.lchat.app.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lchat.app.ui.components.NeumorphicButton
import com.lchat.app.ui.theme.NeuAccent
import com.lchat.app.ui.theme.TextSecondary
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import kotlinx.coroutines.delay

@Composable
fun EmailVerificationScreen(
    onVerified: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val email = viewModel.currentUserEmail
    var resendCooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onVerified()
        }
    }

    // contador regresivo para reenviar
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "✉",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit(64f, androidx.compose.ui.unit.TextUnitType.Sp))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verifica tu email",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hemos enviado un correo a",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = NeuAccent,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pulsa el enlace del correo y luego toca el botón de abajo.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = (uiState as AuthUiState.Error).message,
                style = MaterialTheme.typography.bodyMedium,
                color = NeuAccent,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        NeumorphicButton(
            onClick = { viewModel.checkVerification() },
            modifier = Modifier.fillMaxWidth(),
            accent = true,
            enabled = uiState !is AuthUiState.Loading
        ) {
            Text(
                if (uiState is AuthUiState.Loading) "Verificando..."
                else "Ya verifiqué"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        NeumorphicButton(
            onClick = {
                viewModel.resendVerification()
                resendCooldown = 60
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = resendCooldown == 0
        ) {
            Text(
                if (resendCooldown > 0) "Reenviar en ${resendCooldown}s"
                else "Reenviar correo"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}