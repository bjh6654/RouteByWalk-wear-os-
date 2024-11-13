import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.routeguidance"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.routeguidance"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "TMAP_API_KEY", gradleLocalProperties(rootDir, providers).getProperty("TMAP_API_KEY"))
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
//    implementation(libs.compose.material)
//    implementation("androidx.compose.material:material:1.7.1")
    implementation(libs.material3)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.tiles)
    implementation(libs.tiles.material)
    implementation(libs.tiles.tooling.preview)
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.tiles)
    implementation(libs.watchface.complications.data.source.ktx)
    implementation(libs.play.services.location)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.tiles.tooling)

    implementation(files("libs/tmap-sdk-1.5.aar"))
    implementation(files("libs/vsm-tmap-sdk-v2-android-1.6.60.aar"))
    implementation(libs.material)

    implementation(libs.retrofit) // Retrofit 2.9.0
    implementation(libs.converter.gson) // Gson Converter
    // Coroutines (비동기 처리 라이브러리)
    implementation(libs.kotlinx.coroutines.android) // 최신 Kotlin Coroutines
    // Lifecycle (ViewModel, LiveData 등 UI와 관련된 아키텍처 컴포넌트)
    implementation(libs.androidx.lifecycle.runtime.ktx) // 최신 라이프사이클 라이브러리
}