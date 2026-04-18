package com.lchat.app.chats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lchat.app.data.Message
import com.lchat.app.ui.components.NeumorphicButton
import com.lchat.app.ui.components.NeumorphicTextField
import com.lchat.app.ui.neumorphism.neumorphic
import com.lchat.app.ui.theme.NeuAccent
import com.lchat.app.ui.theme.NeuSurface
import com.lchat.app.ui.theme.OnlineGreen
import com.lchat.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatScreen(
    chatId: String,
    otherUid: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(chatId, otherUid))
) {
    val messages by viewModel.messages.collectAsState()
    val otherUser by viewModel.otherUser.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var messageToDelete by remember { mutableStateOf<Message?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(it) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        ChatHeader(
            username = otherUser?.username?.ifEmpty { otherUser?.email } ?: "...",
            isOnline = otherUser?.isOnline == true,
            onBack = onBack
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isMine = message.senderId == viewModel.currentUid,
                    onLongClick = { messageToDelete = message }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        MessageInput(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                viewModel.sendMessage(inputText)
                inputText = ""
            },
            onPickImage = {
                photoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Eliminar mensaje") },
            text = { Text(msg.text ?: "Este mensaje") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMessage(msg.id)
                    messageToDelete = null
                }) {
                    Text("Eliminar", color = NeuAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = NeuSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun ChatHeader(
    username: String,
    isOnline: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .neumorphic(shape = CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text("<", color = NeuAccent, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NeuSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = NeuAccent
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = if (isOnline) OnlineGreen else TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    onLongClick: () -> Unit
) {
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.75f).dp
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isImage = message.type == "image" && message.fileUrl != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .background(
                    color = if (isMine) NeuAccent else NeuSurface,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .padding(
                    horizontal = if (isImage) 4.dp else 14.dp,
                    vertical = if (isImage) 4.dp else 10.dp
                )
        ) {
            Column {
                if (isImage) {
                    AsyncImage(
                        model = message.fileUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .aspectRatio(1.3f),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.text ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isMine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.createdAt?.let { timeFormat.format(it.toDate()) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        TextSecondary
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .neumorphic(shape = CircleShape)
                .clickable { onPickImage() },
            contentAlignment = Alignment.Center
        ) {
            Text("P", color = NeuAccent, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            NeumorphicTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = "Mensaje..."
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        NeumorphicButton(
            onClick = onSend,
            accent = true
        ) {
            Text(">")
        }
    }
}