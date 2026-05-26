package com.lchat.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.lchat.app.BuildConfig
import com.lchat.app.auth.EmailVerificationScreen
import com.lchat.app.auth.LoginScreen
import com.lchat.app.auth.RegisterScreen
import com.lchat.app.chats.AddMembersScreen
import com.lchat.app.chats.ChatScreen
import com.lchat.app.chats.ChatsListScreen
import com.lchat.app.chats.CreateGroupScreen
import com.lchat.app.chats.FullscreenImageScreen
import com.lchat.app.chats.GroupProfileScreen
import com.lchat.app.chats.NewChatScreen
import com.lchat.app.data.UserRepository
import kotlinx.coroutines.launch
import com.lchat.app.SplashScreen
import com.lchat.app.profile.ProfileScreen
import com.lchat.app.profile.SettingsScreen
import com.lchat.app.profile.UserProfileScreen

object FullscreenImageHolder {
    var url: String = ""
}

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val EMAIL_VERIFICATION = "email_verification"
    const val CHATS = "chats"
    const val NEW_CHAT = "new_chat"
    const val CREATE_GROUP = "create_group"
    const val CHAT = "chat/{chatId}/{otherUid}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val FULLSCREEN_IMAGE = "fullscreen_image"
    const val USER_PROFILE = "user_profile/{uid}"
    const val GROUP_PROFILE = "group_profile/{chatId}"
    const val ADD_MEMBERS = "add_members/{chatId}/{existingUids}"

    fun chat(chatId: String, otherUid: String) = "chat/$chatId/$otherUid"
    fun userProfile(uid: String) = "user_profile/$uid"
    fun groupProfile(chatId: String) = "group_profile/$chatId"
    fun addMembers(chatId: String, existingUids: List<String>) =
        "add_members/$chatId/${existingUids.joinToString(",")}"
}

@Composable
fun LChatNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    val user = Firebase.auth.currentUser
                    val dest = when {
                        user == null -> Routes.LOGIN
                        BuildConfig.REQUIRE_EMAIL_VERIFICATION && !user.isEmailVerified -> Routes.EMAIL_VERIFICATION
                        else -> Routes.CHATS
                    }
                    navController.navigate(dest) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
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
                onVerificationPending = {
                    navController.navigate(Routes.EMAIL_VERIFICATION) {
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
                onVerificationPending = {
                    navController.navigate(Routes.EMAIL_VERIFICATION) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.EMAIL_VERIFICATION) {
            EmailVerificationScreen(
                onVerified = {
                    kotlinx.coroutines.MainScope().launch {
                        UserRepository().setOnline(true)
                    }
                    navController.navigate(Routes.CHATS) {
                        popUpTo(Routes.EMAIL_VERIFICATION) { inclusive = true }
                    }
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
                onUserClick = { uid ->
                    navController.navigate(Routes.userProfile(uid))
                },
                onCreateGroup = {
                    navController.navigate(Routes.CREATE_GROUP)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CREATE_GROUP) {
            CreateGroupScreen(
                onGroupCreated = { chatId ->
                    navController.navigate(Routes.chat(chatId, "group")) {
                        popUpTo(Routes.CHATS)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.USER_PROFILE) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
            UserProfileScreen(
                uid = uid,
                onBack = { navController.popBackStack() },
                onChatOpen = { chatId, otherUid ->
                    navController.navigate(Routes.chat(chatId, otherUid)) {
                        popUpTo(Routes.CHATS)
                    }
                },
                onFullscreenImage = {
                    navController.navigate(Routes.FULLSCREEN_IMAGE)
                }
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
                },
                onProfileClick = { uid ->
                    navController.navigate(Routes.userProfile(uid))
                },
                onGroupClick = { gChatId ->
                    navController.navigate(Routes.groupProfile(gChatId))
                }
            )
        }

        composable(Routes.GROUP_PROFILE) { backStackEntry ->
            val gChatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            GroupProfileScreen(
                chatId = gChatId,
                onBack = { navController.popBackStack() },
                onGroupLeft = {
                    navController.navigate(Routes.CHATS) {
                        popUpTo(Routes.CHATS) { inclusive = false }
                    }
                },
                onMemberClick = { uid ->
                    navController.navigate(Routes.userProfile(uid))
                },
                onAddMembers = { existingUids ->
                    navController.navigate(Routes.addMembers(gChatId, existingUids))
                }
            )
        }

        composable(Routes.ADD_MEMBERS) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val existingUidsStr = backStackEntry.arguments?.getString("existingUids") ?: ""
            val existingUids = existingUidsStr.split(",").filter { it.isNotEmpty() }
            AddMembersScreen(
                chatId = chatId,
                existingMemberUids = existingUids,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
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
                },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}