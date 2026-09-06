package com.zhiwei.math.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.popEnter
import com.zhiwei.math.glass.popExit
import com.zhiwei.math.glass.pushEnter
import com.zhiwei.math.glass.pushExit
import com.zhiwei.math.ui.chat.ChatScreen
import com.zhiwei.math.ui.chatlist.ChatListScreen
import com.zhiwei.math.ui.convoSettings.ConvoSettingsScreen
import com.zhiwei.math.ui.misc.ExampleBookScreen
import com.zhiwei.math.ui.misc.HighlightsScreen
import com.zhiwei.math.ui.onboarding.OnboardingScreen
import com.zhiwei.math.ui.practice.PracticeScreen
import com.zhiwei.math.ui.report.ReportScreen
import com.zhiwei.math.ui.settings.ApiSettingsScreen
import com.zhiwei.math.ui.settings.SettingsScreen
import com.zhiwei.math.ui.subject.SubjectScreen
import com.zhiwei.math.ui.tutorial.TutorialScreen
import org.koin.compose.koinInject

/** 路由表（各页面在后续阶段填充真实实现） */
object Routes {
    const val ONBOARDING = "onboarding"
    const val SUBJECT = "subject"
    const val CHAT_LIST = "chat_list"
    const val CHAT = "chat/{convoId}"
    const val CONVO_SETTINGS = "convo_settings/{convoId}"
    const val PRACTICE = "practice"
    const val EXAMPLE_BOOK = "example_book"
    const val HIGHLIGHTS = "highlights"
    const val REPORT = "report"
    const val SETTINGS = "settings"
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
        true -> Routes.SUBJECT
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
                    navController.navigate(Routes.SUBJECT) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SUBJECT) {
            SubjectScreen(
                onStartMath = { navController.navigate(Routes.CHAT_LIST) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onOpenChat = { convoId -> navController.navigate(Routes.chat(convoId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
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
        composable(Routes.PRACTICE) { PracticeScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.EXAMPLE_BOOK) {
            ExampleBookScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { convoId ->
                    navController.navigate(Routes.chat(convoId))
                },
            )
        }
        composable(Routes.HIGHLIGHTS) { HighlightsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.REPORT) { ReportScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenApiSettings = { navController.navigate(Routes.API_SETTINGS) },
                onOpenPractice = { navController.navigate(Routes.PRACTICE) },
                onOpenExampleBook = { navController.navigate(Routes.EXAMPLE_BOOK) },
                onOpenHighlights = { navController.navigate(Routes.HIGHLIGHTS) },
                onOpenReport = { navController.navigate(Routes.REPORT) },
                onOpenTutorial = { navController.navigate(Routes.TUTORIAL) },
            )
        }
        composable(Routes.API_SETTINGS) {
            ApiSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TUTORIAL) { TutorialScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
private fun Placeholder(label: String) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
