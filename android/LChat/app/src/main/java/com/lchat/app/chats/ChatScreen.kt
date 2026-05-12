package com.lchat.app.chats

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lchat.app.data.Message
import com.lchat.app.ui.components.NeumorphicTextField
import com.lchat.app.ui.neumorphism.neumorphic
import com.lchat.app.ui.theme.NeuAccent
import com.lchat.app.ui.theme.NeuSurface
import com.lchat.app.ui.theme.OnlineGreen
import com.lchat.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024     -> "%.0f KB".format(bytes / 1_024.0)
        else               -> "$bytes B"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    otherUid: String,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onProfileClick: (uid: String) -> Unit = {},
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(chatId, otherUid))
) {
    val messages by viewModel.messages.collectAsState()
    val otherUser by viewModel.otherUser.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var menuMessage by remember { mutableStateOf<Message?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(it) }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val cursor = context.contentResolver.query(uri, null, null, null, null)
        var fileName = "file"
        var fileSize = 0L
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) fileName = it.getString(nameIdx) ?: "file"
                if (sizeIdx >= 0) fileSize = it.getLong(sizeIdx)
            }
        }

        if (fileSize > 20 * 1024 * 1024) return@rememberLauncherForActivityResult

        viewModel.sendFile(uri, fileName, fileSize)
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
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))
    ) {
        ChatHeader(
            username = otherUser?.username?.ifEmpty { otherUser?.email } ?: "...",
            avatarUrl = otherUser?.avatarUrl,
            isOnline = otherUser?.isOnline == true,
            onBack = onBack,
            onAvatarClick = { onImageClick(otherUser?.avatarUrl ?: return@ChatHeader) },
            onUsernameClick = { onProfileClick(otherUid) }
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
                    onLongClick = { menuMessage = message },
                    onFileTap = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    onImageTap = { url -> onImageClick(url) }
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
            },
            onPickFile = {
                fileLauncher.launch(arrayOf("*/*"))
            }
        )
    }

    menuMessage?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { menuMessage = null },
            sheetState = sheetState,
            containerColor = NeuSurface
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {

                if (msg.type == "image" || msg.type == "file") {
                    MenuOption(
                        icon = Icons.Default.Download,
                        label = "Descargar",
                        iconTint = MaterialTheme.colorScheme.onSurface
                    ) {
                        val url = msg.fileUrl ?: return@MenuOption
                        val fileName = when (msg.type) {
                            "image" -> "lchat_photo_${msg.id}.jpg"
                            else    -> msg.fileName ?: "lchat_file_${msg.id}"
                        }
                        downloadFile(context, url, fileName)
                        menuMessage = null
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }

                MenuOption(
                    icon = Icons.Default.Delete,
                    label = "Eliminar para mi",
                    iconTint = NeuAccent
                ) {
                    viewModel.deleteMessage(msg.id, deleteForAll = false)
                    menuMessage = null
                }

                if (msg.senderId == viewModel.currentUid) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    MenuOption(
                        icon = Icons.Default.DeleteForever,
                        label = "Eliminar para todos",
                        iconTint = NeuAccent
                    ) {
                        viewModel.deleteMessage(msg.id, deleteForAll = true)
                        menuMessage = null
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun downloadFile(context: Context, url: String, fileName: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(fileName)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}

@Composable
private fun ChatHeader(
    username: String,
    avatarUrl: String?,
    isOnline: Boolean,
    onBack: () -> Unit,
    onAvatarClick: () -> Unit,
    onUsernameClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
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

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NeuSurface)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = NeuAccent
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.clickable { onUsernameClick() }) {
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
    onLongClick: () -> Unit,
    onFileTap: (String) -> Unit,
    onImageTap: (String) -> Unit
) {
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.75f).dp
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isImage = message.type == "image" && message.fileUrl != null
    val isFile = message.type == "file" && message.fileUrl != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .combinedClickable(
                    onClick = {
                        when {
                            isImage -> message.fileUrl?.let { onImageTap(it) }
                            isFile  -> message.fileUrl?.let { onFileTap(it) }
                        }
                    },
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
                when {
                    isImage -> {
                        AsyncImage(
                            model = message.fileUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .aspectRatio(1.3f),
                            contentScale = ContentScale.Crop
                        )
                    }
                    isFile -> {
                        FileBubbleContent(
                            fileName = message.fileName ?: "Archivo",
                            fileSize = message.fileSize,
                            isMine = isMine
                        )
                    }
                    else -> {
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
private fun FileBubbleContent(
    fileName: String,
    fileSize: Long?,
    isMine: Boolean
) {
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.8f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (fileSize != null && fileSize > 0) {
                Text(
                    text = formatFileSize(fileSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.6f)
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
    onPickImage: () -> Unit,
    onPickFile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón foto
        Box(
            modifier = Modifier
                .size(40.dp)
                .neumorphic(shape = CircleShape)
                .clickable { onPickImage() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Foto",
                tint = NeuAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Botón archivo
        Box(
            modifier = Modifier
                .size(40.dp)
                .neumorphic(shape = CircleShape)
                .clickable { onPickFile() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Archivo",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
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

        // Botón enviar
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color = NeuAccent, shape = CircleShape)
                .clickable { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}