// OpenCV 4.6.0 Android SDK（预编译 .so + Java 绑定），作为源码模块导入。
// 原 sdk/build.gradle 为上古 Gradle 语法，已替换为本文件。
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.opencv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        // OpenCV 源码引用 org.opencv.BuildConfig
        buildConfig = true
        // 不开 aidl：aidl.exe 无法处理含中文的工程路径；
        // 唯一引用 AIDL 类的 AsyncServiceHelper（OpenCV Engine 服务路径）已被排除，
        // 本项目走 OpenCVLoader.initDebug() 本地库加载，不经过该服务。
    }

    sourceSets {
        getByName("main") {
            java.srcDir("java/src")
            // attrs.xml（CameraBridgeViewBase 的 styleable）
            res.srcDir("java/res")
            jniLibs.srcDir("native/libs")
        }
    }
}
