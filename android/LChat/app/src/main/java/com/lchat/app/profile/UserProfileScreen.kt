package com.lchat.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lchat.app.navigation.FullscreenImageHolder
import com.lchat.app.ui.components.NeumorphicButton
import com.lchat.app.ui.neumorphism.neumorphic
import com.lchat.app.ui.theme.NeuAccent
import com.lchat.app.ui.theme.NeuSurface
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun UserProfileScreen(
    uid: String,
    onBack: () -> Unit,
    onChatOpen: (chatId: String, otherUid: String) -> Unit,
    onFullscreenImage: () -> Unit,
    viewModel: UserProfileViewModel = viewModel(factory = UserProfileViewModel.Factory(uid))
) {
    val user by viewModel.user.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UserProfileUiState.ChatReady) {
            val state = uiState as UserProfileUiState.ChatReady
            onChatOpen(state.chatId, state.otherUid)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(NeuSurface)
                    .clickable {
                        user?.avatarUrl?.let { url ->
                            FullscreenImageHolder.url = url
                            onFullscreenImage()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (user?.avatarUrl != null) {
                    AsyncImage(
                        model = user!!.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user?.username?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = NeuAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = user?.username ?: "...",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (uiState is UserProfileUiState.Error) {
                Text(
                    text = (uiState as UserProfileUiState.Error).message,
                    color = NeuAccent,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            NeumorphicButton(
                onClick = { viewModel.startChat() },
                modifier = Modifier.fillMaxWidth(),
                accent = true,
                enabled = uiState !is UserProfileUiState.Loading
            ) {
                if (uiState is UserProfileUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Escribir mensaje")
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp, start = 16.dp)
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
    }
}