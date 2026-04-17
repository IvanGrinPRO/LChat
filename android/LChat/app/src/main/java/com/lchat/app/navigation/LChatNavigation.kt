package com.lchat.app.navigation

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
import com.lchat.app.chats.NewChatScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHATS = "chats"
    const val NEW_CHAT = "new_chat"
    const val CHAT = "chat/{chatId}"

    fun chat(chatId: String) = "chat/$chatId"
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
                onChatClick = { chatId ->
                    navController.navigate(Routes.chat(chatId))
                },
                onNewChatClick = {
                    navController.navigate(Routes.NEW_CHAT)
                },
                onLogoutClick = {
                    Firebase.auth.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.NEW_CHAT) {
            NewChatScreen(
                onChatCreated = { chatId ->
                    navController.navigate(Routes.chat(chatId)) {
                        popUpTo(Routes.CHATS)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CHAT) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}