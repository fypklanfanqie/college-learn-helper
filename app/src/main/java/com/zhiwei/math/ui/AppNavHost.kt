package com.zhiwei.math.ui

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.popEnter
import com.zhiwei.math.glass.popExit
import com.zhiwei.math.glass.pushEnter
import com.zhiwei.math.glass.pushExit
import com.zhiwei.math.ui.chat.ChatScreen
import com.zhiwei.math.ui.chatlist.ChatListScreen
import com.zhiwei.math.ui.convoSettings.ConvoSettingsScreen
import com.zhiwei.math.ui.main.MainScaffold
import com.zhiwei.math.ui.onboarding.OnboardingScreen
import com.zhiwei.math.ui.settings.ApiSettingsScreen
import com.zhiwei.math.ui.tutorial.TutorialScreen
import org.koin.compose.koinInject

/** 路由表：MAIN 为 dock 主界面；对话/对话设置/API/教程为 push 详情页 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val CHAT_LIST = "chat_list"
    const val CHAT = "chat/{convoId}"
    const val CONVO_SETTINGS = "convo_settings/{convoId}"
    const val API_SETTINGS = "api_settings"
    const val TUTORIAL = "tutorial"

    fun chat(convoId: Long) = "chat/$convoId"
    fun convoSettings(convoId: Long) = "convo_settings/$convoId"
}

@Composable
fun AppNavHost(settings: SettingsStore = koinInject()) {
    // null = 尚未读到位（避免闪烁）；true = 已配置；false = 走 Onboarding
    val onboardingDone by settings.onboardingDone.collectAsState(initial = null)

    val startDestination = when (onboardingDone) {
        null -> null
        false -> Routes.ONBOARDING
        true -> Routes.MAIN
    }

    val nav = startDestination ?: return
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = nav,
        enterTransition = { pushEnter() },
        exitTransition = { pushExit() },
        popEnterTransition = { popEnter() },
        popExitTransition = { popExit() },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            MainScaffold(
                onOpenChatList = { navController.navigate(Routes.CHAT_LIST) },
                onOpenChat = { convoId -> navController.navigate(Routes.chat(convoId)) },
                onOpenApiSettings = { navController.navigate(Routes.API_SETTINGS) },
                onOpenTutorial = { navController.navigate(Routes.TUTORIAL) },
            )
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onOpenChat = { convoId -> navController.navigate(Routes.chat(convoId)) },
            )
        }
        composable(Routes.CHAT) { entry ->
            ChatScreen(
                onBack = { navController.popBackStack() },
                onOpenConvoSettings = {
                    val convoId = entry.arguments?.getString("convoId")?.toLongOrNull() ?: -1L
                    navController.navigate(Routes.convoSettings(convoId))
                },
            )
        }
        composable(Routes.CONVO_SETTINGS) { entry ->
            ConvoSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.API_SETTINGS) {
            ApiSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TUTORIAL) { TutorialScreen(onBack = { navController.popBackStack() }) }
    }
}
