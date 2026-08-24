import java.io.ByteArrayOutputStream
import java.io.File

plugins {
    alias(libs.plugins.android.library)
}

val webUiDir = rootProject.layout.projectDirectory.dir("web-ui")
val webStaticResourcesDir = layout.projectDirectory.dir("src/main/resources/static")

val buildWebUi = tasks.register("buildWebUi") {
    group = "build"
    description = "Build web-ui and copy its static output into the web module resources."

    val webUiDirFile = webUiDir.asFile
    val staticDirFile = webStaticResourcesDir.asFile

    inputs.files(
        webUiDir.file("package.json"),
        webUiDir.file("pnpm-lock.yaml"),
        webUiDir.file("components.json"),
        webUiDir.file("copy.ts"),
        webUiDir.file("react-router.config.ts"),
        webUiDir.file("tsconfig.json"),
        webUiDir.file("vite.config.ts"),
        webUiDir.file("vite-env.d.ts")
    )
    inputs.dir(webUiDir.dir("app"))
    inputs.dir(webUiDir.dir("public"))
    outputs.dir(webStaticResourcesDir)

    /** 使用原生 ProcessBuilder 执行命令，绕开 Gradle 不同版本 exec DSL 的 API 兼容问题。 */
    fun runCommand(
        cmd: Array<String>,
        workingDir: File? = null,
        captureStdout: Boolean = false,
        ignoreExit: Boolean = true,
        inheritIO: Boolean = !captureStdout,
    ): Pair<Int, String> {
        val builder = ProcessBuilder(*cmd)
        if (workingDir != null) builder.directory(workingDir)
        builder.redirectErrorStream(true)
        if (inheritIO) {
            builder.inheritIO()
        }
        val process = builder.start()
        val stdout = ByteArrayOutputStream()
        if (captureStdout || !inheritIO) {
            process.inputStream.copyTo(stdout)
        }
        val exit = process.waitFor()
        if (!ignoreExit && exit != 0) {
            error("Command failed (exit=$exit): ${cmd.joinToString(" ")}")
        }
        return exit to stdout.toString(Charsets.UTF_8)
    }

    doLast {
        fun findStaticFilesCount(dir: File): Int =
            if (dir.isDirectory) dir.walkTopDown().filter { it.isFile }.count() else 0

        fun isWindows(): Boolean =
            System.getProperty("os.name")?.contains("win", ignoreCase = true) == true

        fun isMac(): Boolean =
            System.getProperty("os.name")?.contains("mac", ignoreCase = true) == true

        val currentStaticCount = findStaticFilesCount(staticDirFile)

        val skipByProperty = providers.gradleProperty("rikkahub.skipWebBuild")
            .orNull?.toString().toBoolean()
        val skipByEnv = providers.environmentVariable("RIKKAHUB_SKIP_WEB_BUILD")
            .orNull?.toString().toBoolean()
        val ciEnv = providers.environmentVariable("CI")
            .orNull?.toString().toBoolean()
        val skipWebBuild = skipByProperty || skipByEnv || (ciEnv && currentStaticCount >= 10)

        val pnpmAvailable = runCatching {
            val checkCmd = when {
                isWindows() -> arrayOf("cmd", "/c", "where pnpm")
                else -> arrayOf("sh", "-c", "command -v pnpm")
            }
            val (_, out) = runCommand(checkCmd, captureStdout = true, ignoreExit = true)
            out.isNotBlank()
        }.getOrDefault(false)

        when {
            skipWebBuild -> {
                logger.lifecycle("[web] Skipping web-ui build (rikkahub.skipWebBuild or CI with existing static resources). existingStaticFiles=$currentStaticCount")
                if (currentStaticCount < 1) {
                    error("[web] Skip requested but no static resources exist at $staticDirFile. Please run 'pnpm -C web-ui install && pnpm -C web-ui run build' locally and commit the output, or enable web-ui build.")
                }
            }

            !pnpmAvailable -> {
                if (currentStaticCount >= 10) {
                    logger.warn("[web] pnpm not found on PATH. Skipping web-ui build and reusing existing static resources (count=$currentStaticCount). Install pnpm if you need to re-generate the web frontend.")
                } else {
                    error("[web] pnpm not found on PATH and no prebuilt static resources exist (count=$currentStaticCount). Install pnpm and re-run, or commit prebuilt resources to web/src/main/resources/static/.")
                }
            }

            else -> {
                logger.lifecycle("[web] Building web-ui with pnpm...")
                val buildCmd = when {
                    isMac() -> arrayOf("zsh", "-ic", "pnpm install --frozen-lockfile --prefer-offline && pnpm run build")
                    isWindows() -> arrayOf("cmd", "/c", "pnpm install --frozen-lockfile --prefer-offline && pnpm run build")
                    else -> arrayOf("sh", "-c", "pnpm install --frozen-lockfile --prefer-offline && pnpm run build")
                }
                val (exit, _) = runCommand(buildCmd, workingDir = webUiDirFile, ignoreExit = true, inheritIO = true)
                if (exit != 0) {
                    if (currentStaticCount >= 10) {
                        logger.warn("[web] pnpm build failed (exit=$exit). Falling back to existing prebuilt static resources (count=$currentStaticCount). The built-in web server will still serve these files.")
                    } else {
                        error("[web] pnpm build failed (exit=$exit) AND no prebuilt static resources exist (count=$currentStaticCount). Aborting.")
                    }
                }
            }
        }
    }
}

android {
    namespace = "me.rerere.rikkahub.web"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.named("preBuild") {
    dependsOn(buildWebUi)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ktor server
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.conditional.headers)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.cors)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.sse)
    api(libs.ktor.server.cio)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
