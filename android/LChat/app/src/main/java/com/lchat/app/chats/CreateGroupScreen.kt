package com.lchat.app.chats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import com.lchat.app.data.User
import com.lchat.app.ui.components.NeumorphicButton
import com.lchat.app.ui.components.NeumorphicTextField
import com.lchat.app.ui.neumorphism.neumorphic
import com.lchat.app.ui.theme.NeuAccent
import com.lchat.app.ui.theme.NeuSurface
import com.lchat.app.ui.theme.TextSecondary

@Composable
fun CreateGroupScreen(
    onGroupCreated: (chatId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateGroupViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val selectedUids by viewModel.selectedUids.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is CreateGroupUiState.Created) {
            onGroupCreated((uiState as CreateGroupUiState.Created).chatId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
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
                text = "Nuevo grupo",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // nombre del grupo
        NeumorphicTextField(
            value = groupName,
            onValueChange = { viewModel.onGroupNameChange(it) },
            placeholder = "Nombre del grupo"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // buscador de miembros
        NeumorphicTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = "Buscar miembros..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedUids.isNotEmpty()) {
            Text(
                text = "${selectedUids.size} seleccionado(s)",
                style = MaterialTheme.typography.labelSmall,
                color = NeuAccent,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (uiState is CreateGroupUiState.Error) {
            Text(
                text = (uiState as CreateGroupUiState.Error).message,
                style = MaterialTheme.typography.bodyMedium,
                color = NeuAccent,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.uid }) { user ->
                MemberItem(
                    user = user,
                    selected = user.uid in selectedUids,
                    onClick = { viewModel.toggleUser(user.uid) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        NeumorphicButton(
            onClick = { viewModel.createGroup() },
            modifier = Modifier.fillMaxWidth(),
            accent = true,
            enabled = uiState !is CreateGroupUiState.Creating
        ) {
            if (uiState is CreateGroupUiState.Creating) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("Crear grupo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MemberItem(
    user: User,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NeuSurface),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user.username.take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    color = NeuAccent
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = user.username.ifEmpty { user.email },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) NeuAccent else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}