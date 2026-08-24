pluginManagement {
    includeBuild("build-logic")

    repositories {
        // Google 官方 Maven 优先（AGP 及 com.android.* 系列插件均发布于此，最可靠）
        // 注意：不使用阿里云 gradle-plugin 镜像 —— 它未同步 com.android.test 等插件。
        // 不使用 content filter —— CI 上曾因 filter 把 com.android.test 排除导致插件解析失败。
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.itextsupport.com/android")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.objectbox") {
                useModule("io.objectbox:objectbox-gradle-plugin:${requested.version}")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内镜像优先，加速依赖下载（官方仓库作为回退保留在后面）
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
    }
}

rootProject.name = "rikkahub"
include(":app")
include(":highlight")
include(":ai")
include(":search")
include(":speech")
include(":common")
include(":document")
include(":web")
include(":material3")
include(":workspace")
include(":app:baselineprofile")
