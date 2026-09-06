// RapidOcrAndroidOnnxCompose 的 OcrLibrary（Apache-2.0）源码模块。
// 原 Groovy build.gradle 面向 AGP 8.6.1 + Kotlin 1.7，已按本项目版本线（AGP 9.3 / Kotlin 2.4）重写。
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.benjaminwan.ocrlibrary"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation(libs.androidx.core.ktx)
    implementation(libs.onnxruntime)
    implementation(libs.logger)
    implementation(project(":opencv"))
}
