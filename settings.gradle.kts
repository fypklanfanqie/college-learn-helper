pluginManagement {
    repositories {
        // 闃块噷浜戦暅鍍忎紭鍏堬紙鍥藉唴鐩磋繛绋冲畾锛夛紱瀹樻柟浠撳簱鍏滃簳
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "ZhiweiMath"

include(":app")
include(":glasense-ui")
include(":ocr-library")
include(":opencv")
project(":opencv").projectDir = file("opencv")
