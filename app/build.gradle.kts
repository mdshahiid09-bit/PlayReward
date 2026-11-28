import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

val githubOwner = (project.findProperty("PLAYREWARD_GITHUB_OWNER") as? String) ?: "mdshahiid09-bit"
val githubRepo = (project.findProperty("PLAYREWARD_GITHUB_REPO") as? String) ?: "PlayReward"

android {
    namespace = "com.playreward.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.playreward.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 16
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "GITHUB_OWNER", "\"$githubOwner\"")
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("KEYSTORE_FILE") ?: keystoreProps.getProperty("storeFile")
            val storePass = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
            val alias = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
            val keyPass = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
            if (!storePath.isNullOrBlank() && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val relSigning = signingConfigs.getByName("release")
            if (relSigning.storeFile != null) {
                signingConfig = relSigning
            }
        }
        debug {
            isMinifyEnabled = false
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Firebase (Spark Free Plan: Auth + Realtime Database)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // DataStore for Keep Me Logged In, Theme, Onboarding
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil for async images (banners, task icons, support logos, popup image)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    // WorkManager for local push notifications without FCM
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // OkHttp for GitHub Releases update checker
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines Play Services
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
