package com.lchat.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.lchat.app.auth.LoginScreen
import com.lchat.app.auth.RegisterScreen
import com.lchat.app.chats.ChatScreen
import com.lchat.app.chats.ChatsListScreen
import com.lchat.app.chats.FullscreenImageScreen
import com.lchat.app.chats.NewChatScreen
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.launch
import com.lchat.app.profile.ProfileScreen

// URL guardada en memoria para evitar problemas de encoding en Navigation
object FullscreenImageHolder {
    var url: String = ""
}

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHATS = "chats"
    const val NEW_CHAT = "new_chat"
    const val CHAT = "chat/{chatId}/{otherUid}"
    const val PROFILE = "profile"
    const val FULLSCREEN_IMAGE = "fullscreen_image"

    fun chat(chatId: String, otherUid: String) = "chat/$chatId/$otherUid"
}

@Composable
fun LChatNavigation() {
    val navController = rememberNavController()

    val startDestination = if (Firebase.auth.currentUser != null) {
        Routes.CHATS
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    kotlinx.coroutines.MainScope().launch {
                        UserRepository().setOnline(true)
                    }
                    navController.navigate(Routes.CHATS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    kotlinx.coroutines.MainScope().launch {
                        UserRepository().setOnline(true)
                    }
                    navController.navigate(Routes.CHATS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.CHATS) {
            ChatsListScreen(
                onChatClick = { chatId, otherUid ->
                    navController.navigate(Routes.chat(chatId, otherUid))
                },
                onNewChatClick = {
                    navController.navigate(Routes.NEW_CHAT)
                },
                onLogoutClick = {
                    kotlinx.coroutines.MainScope().launch {
                        UserRepository().setOnline(false)
                        Firebase.auth.signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onProfileClick = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        composable(Routes.NEW_CHAT) {
            NewChatScreen(
                onChatCreated = { chatId, otherUid ->
                    navController.navigate(Routes.chat(chatId, otherUid)) {
                        popUpTo(Routes.CHATS)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CHAT) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val otherUid = backStackEntry.arguments?.getString("otherUid") ?: return@composable
            ChatScreen(
                chatId = chatId,
                otherUid = otherUid,
                onBack = { navController.popBackStack() },
                onImageClick = { url ->
                    FullscreenImageHolder.url = url
                    navController.navigate(Routes.FULLSCREEN_IMAGE)
                }
            )
        }

        composable(Routes.FULLSCREEN_IMAGE) {
            FullscreenImageScreen(
                imageUrl = FullscreenImageHolder.url,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    kotlinx.coroutines.MainScope().launch {
                        UserRepository().setOnline(false)
                        Firebase.auth.signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}