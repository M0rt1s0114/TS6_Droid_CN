import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.tsdroid.han"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yuaxi.ts6droid.cn"
        minSdk = 29
        targetSdk = 36
        versionCode = 14
        versionName = "2.1.3-2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (file("${rootDir}/release.keystore").exists()) {
            create("release") {
                storeFile = file("${rootDir}/release.keystore")
                storePassword = "ts6droid"
                keyAlias = "ts6droid"
                keyPassword = "ts6droid"
            }
        }
    }

    buildTypes {
        debug {
            if (signingConfigs.names.contains("release")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingConfigs.names.contains("release")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", "src/main/kotlin")
        }
    }

    // Android Lint 静态检查配置
    // 运行方式：Android Studio 右侧 Gradle 面板 -> app -> verification -> lintDebug
    // 或命令行：.\gradlew.bat :app:lintDebug
    // 报告位置：app/build/reports/lint-results-debug.html / .txt / .xml / .sarif
    lint {
        // 新手友好：lint 发现问题时仍能正常编译，问题会在报告里完整列出
        abortOnError = false
        checkReleaseBuilds = false
        warningsAsErrors = false

        // 明确开启所有常见报告格式
        htmlReport = true
        xmlReport = true
        textReport = true
        sarifReport = true

        // 同时检查第三方依赖库里的问题
        checkDependencies = true

        // 关闭与项目实际情况不相关、或纯“催促升级依赖”的噪声规则
        disable += setOf(
            "GradleDependency",           // “有新版依赖可用”只是提示，不是代码问题
            "AndroidGradlePluginVersion", // 同上：AGP 版本升级是发布决策，不阻塞日常检查
            "OldTargetApi",               // targetSdk 由发布计划决定，不是代码缺陷
            "AppBundleLocaleChanges",     // 本项目通过 GitHub 分发 APK，不打 App Bundle
            "ObsoleteSdkInt",             // adaptive-icon 仍需 -v26 目录，AAPT2 不接受无版本限定
            "TypographyQuotes",           // 中文文案不需要英文弯引号规则
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Task to build Rust native libraries via cargo-ndk
tasks.register<Exec>("buildRustLibs") {
    workingDir = file("${rootDir}/../tslib_multi")
    environment("ANDROID_NDK_HOME", System.getenv("ANDROID_NDK_HOME")
        ?: "${System.getProperty("user.home")}/Android/Sdk/ndk/27.2.12479018")
    environment("ANDROID_NDK", System.getenv("ANDROID_NDK_HOME")
        ?: "${System.getProperty("user.home")}/Android/Sdk/ndk/27.2.12479018")
    environment("CMAKE_POLICY_VERSION_MINIMUM", "3.5")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-t", "x86_64",
        "-o", "${projectDir}/src/main/jniLibs",
        "build", "--release", "-p", "tslib-jni",
        "--features", "vendored-openssl",
        "-j10"
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    debugImplementation(libs.androidx.ui.tooling)
}
