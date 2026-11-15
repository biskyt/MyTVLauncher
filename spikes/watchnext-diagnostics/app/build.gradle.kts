plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.biskyt.mytvlauncher.diag"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.biskyt.mytvlauncher.diag"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1"
    }
    // The launcher's shared test key, so the debug build installs without one of its own.
    signingConfigs.getByName("debug") { storeFile = file("../../../app/debug.keystore") }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }

dependencies {
    testImplementation(libs.junit)
}
