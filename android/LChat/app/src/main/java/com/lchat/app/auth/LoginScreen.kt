package com.lchat.app.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lchat.app.ui.components.NeumorphicButton
import com.lchat.app.ui.components.NeumorphicTextField
import com.lchat.app.ui.theme.NeuAccent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.lchat.app.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_login),
                contentDescription = "LChat",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            NeumorphicTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                keyboardType = KeyboardType.Email
            )

            NeumorphicTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                isPassword = true
            )

            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = NeuAccent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NeumorphicButton(
                onClick = { viewModel.signIn(email, password) },
                modifier = Modifier.fillMaxWidth(),
                accent = true,
                enabled = uiState !is AuthUiState.Loading
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text("Sign In")
                }
            }

            NeumorphicButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Account")
            }
        }
    }
}