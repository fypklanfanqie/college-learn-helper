package com.zhiwei.math

import android.app.Application
import android.widget.Toast
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.zhiwei.math.di.appModule
import com.zhiwei.math.data.prefs.SettingsStore
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ZhiweiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ZhiweiApp)
            modules(appModule)
        }
        // pdfbox-android 资源初始化（文档上传 PDF 提取）
        PDFBoxResourceLoader.init(applicationContext)

        // 液态玻璃崩溃探针检测（SIGSEGV 是 native 崩溃 catch 不住）：
        // 上次会话 LIQUID 首帧渲染前打了点却未清除 = 渲染线程崩溃 → 禁用液态玻璃。
        // runBlocking：必须在首帧渲染前完成检测（DataStore 首读很快，~ms 级）。
        val store = SettingsStore(this)
        val crashed = runBlocking { store.consumeBootProbe() }
        if (crashed) {
            Toast.makeText(
                this,
                "液态玻璃渲染崩溃，已自动降级为毛玻璃（可在设置中重试）",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
