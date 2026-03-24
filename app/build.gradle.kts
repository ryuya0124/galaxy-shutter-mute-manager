// Galaxy Shutter Mute Manager アプリモジュールの build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "net.ryuya.dev.galaxyshutter.mute.manager"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.ryuya.dev.galaxyshutter.mute.manager"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // AndroidX コア
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Shizuku（インストール・権限付与に使用）
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // ネットワーク（GitHub Releases API）
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // 設定の永続化
    implementation(libs.datastore.preferences)

    // コルーチン
    implementation(libs.kotlinx.coroutines.android)

    // 画像読み込み
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
