plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.lonx.ecjtu.autologin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lonx.ecjtu.autologin"
        minSdk = 24
        targetSdk = 34
        versionCode = 2500
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation (libs.androidx.material)
    implementation (libs.androidx.runtime.livedata)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.material.v180)
    implementation(libs.androidx.lifecycle.runtime.ktx.v285)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.okhttp.v472)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}