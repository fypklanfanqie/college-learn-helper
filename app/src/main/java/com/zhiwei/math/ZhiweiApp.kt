package com.zhiwei.math

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.zhiwei.math.di.appModule
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
    }
}
