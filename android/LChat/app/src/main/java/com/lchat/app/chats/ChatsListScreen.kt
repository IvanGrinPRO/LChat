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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lchat.app.ui.neumorphism.neumorphic
import com.lchat.app.ui.theme.NeuAccent
import com.lchat.app.ui.theme.NeuSurface
import com.lchat.app.ui.theme.OnlineGreen
import com.lchat.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun ChatsListScreen(
    onChatClick: (chatId: String, otherUid: String) -> Unit,
    onNewChatClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatsViewModel = viewModel()
) {
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .neumorphic(shape = CircleShape)
                        .clickable { onNewChatClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = NeuAccent, style = MaterialTheme.typography.titleMedium)
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .neumorphic(shape = CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("P", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeuAccent)
                }
            }
            chats.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin chats. Pulsa + para empezar.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chats, key = { it.chat.id }) { preview ->
                        ChatItem(
                            preview = preview,
                            onClick = { onChatClick(preview.chat.id, preview.otherUser.uid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatItem(
    preview: ChatPreview,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(NeuSurface),
                contentAlignment = Alignment.Center
            ) {
                if (preview.otherUser.avatarUrl != null) {
                    AsyncImage(
                        model = preview.otherUser.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = preview.otherUser.username.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = NeuAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preview.otherUser.username.ifEmpty { preview.otherUser.email },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preview.chat.lastMessageText ?: "Sin mensajes",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        preview.chat.lastMessageAt?.let { timestamp ->
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            Text(
                text = sdf.format(timestamp.toDate()),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}