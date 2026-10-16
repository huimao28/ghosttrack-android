plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.huimao.ghosttrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.huimao.ghosttrack"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // 固定签名：与灰猫地图同一把 release key。
    // keystore 与密码走环境变量（CI 从 Secrets 还原），本地没有 keystore 时
    // release 包保持未签名，debug 构建不受影响。
    signingConfigs {
        create("release") {
            val ksPath = System.getenv("HUIMAO_KEYSTORE_PATH") ?: "../keystore/release.jks"
            if (file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("HUIMAO_KEYSTORE_PASSWORD")
                    ?: providers.gradleProperty("HUIMAO_KEYSTORE_PASSWORD").orNull
                keyAlias = System.getenv("HUIMAO_KEY_ALIAS") ?: "release"
                keyPassword = System.getenv("HUIMAO_KEY_PASSWORD")
                    ?: providers.gradleProperty("HUIMAO_KEY_PASSWORD").orNull
                    ?: storePassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val bom = libs.androidx.compose.bom
    implementation(platform(bom))
    androidTestImplementation(platform(bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.libphonenumber)
    implementation(libs.libphonenumber.carrier)
    implementation(libs.libphonenumber.geocoder)

    implementation(libs.billing.ktx)

    testImplementation(libs.junit)
}
