package com.zhiwei.math.di

import com.zhiwei.math.data.db.AppDatabase
import com.zhiwei.math.data.prefs.ApiKeyStore
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.data.repo.ChatRepository
import com.zhiwei.math.llm.LlmRepository
import com.zhiwei.math.ocr.OcrManager
import com.zhiwei.math.ui.chat.ChatViewModel
import com.zhiwei.math.ui.chatlist.ChatListViewModel
import com.zhiwei.math.ui.convoSettings.ConvoSettingsViewModel
import com.zhiwei.math.ui.misc.ExampleBookViewModel
import com.zhiwei.math.ui.misc.HighlightsViewModel
import com.zhiwei.math.ui.onboarding.OnboardingViewModel
import com.zhiwei.math.ui.practice.PracticeViewModel
import com.zhiwei.math.ui.report.ReportViewModel
import com.zhiwei.math.ui.settings.ApiSettingsViewModel
import com.zhiwei.math.ui.settings.SettingsViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // SSE 长流：读不超时，靠打断取消
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }
    single { SettingsStore(androidContext()) }
    single { ApiKeyStore(androidContext()) }
    single { AppDatabase.get(androidContext()) }
    single { get<AppDatabase>().conversationDao() }
    single { get<AppDatabase>().messageDao() }
    single { get<AppDatabase>().highlightDao() }
    single { get<AppDatabase>().exampleDao() }
    single { get<AppDatabase>().practiceDao() }
    single { LlmRepository(get(), get(), get()) }
    single { OcrManager(androidContext()) }
    single { ChatRepository(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::ApiSettingsViewModel)
    viewModelOf(::ChatListViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ConvoSettingsViewModel)
    viewModelOf(::PracticeViewModel)
    viewModelOf(::HighlightsViewModel)
    viewModelOf(::ExampleBookViewModel)
    viewModelOf(::ReportViewModel)
}
