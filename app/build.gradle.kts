plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.biskyt.mytvlauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.biskyt.mytvlauncher"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = 36
        versionCode = 13
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Only the languages the launcher has words for: libraries' others would add size for nothing.
    androidResources { localeFilters += listOf("en", "en-rGB", "en-rUS", "es", "pt", "pt-rBR", "fr") }

    signingConfigs {
        // A shared, public debug key (MyPlexPlayer's), so builds from any machine (or CI) install over each other.
        // For distribution, set MYTVLAUNCHER_KEYSTORE and MYTVLAUNCHER_KEYSTORE_PASSWORD (MYTVLAUNCHER_KEY_ALIAS defaults
        // to "mytvlauncher" and MYTVLAUNCHER_KEY_PASSWORD to the keystore password, as keytool uses one password for both).
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val store = System.getenv("MYTVLAUNCHER_KEYSTORE")
            if (store != null) {
                storeFile = file(store)
                storePassword = System.getenv("MYTVLAUNCHER_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MYTVLAUNCHER_KEY_ALIAS") ?: "mytvlauncher"
                keyPassword = System.getenv("MYTVLAUNCHER_KEY_PASSWORD") ?: storePassword
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Room's exported schemas, so migration tests can open old database versions.
    sourceSets["debug"].assets.srcDir("$projectDir/schemas")

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // A test JVM per two cores, up to 4 (each needs about 1 GB).
            it.maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
            it.systemProperty("robolectric.graphicsMode", "NATIVE")
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

// Unit tests run against debug only: a release run would only repeat them.
androidComponents {
    beforeVariants(selector().withBuildType("release")) {
        it.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
    }
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.tv.material)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.tvprovider)
    // Pictures that other apps give to Watch Next (posters and thumbnails), loaded at card size with a disk cache.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)

    // Smoke tests on a real Android (an emulator on CI): the home app and the TV provider, which Robolectric can't show.
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
