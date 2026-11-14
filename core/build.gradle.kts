plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.animalsniffer)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    // The Java APIs of Android at the app's minSdk, for Animal Sniffer (below).
    signature("com.toasttab.android:gummy-bears-api-${libs.versions.minSdk.get()}:${libs.versions.gummyBears.get()}@signature")
    api(libs.coroutines.core)

    testImplementation(libs.junit)
}

// Core runs on Android, but lint does not check API levels in a plain JVM module. Check core's classes against the
// Java APIs of Android at the app's minSdk. It runs with core's tests, so CI and local test runs both check it.
animalsniffer {
    sourceSets = listOf(project.sourceSets.main.get())
}
tasks.test { dependsOn(tasks.named("animalsnifferMain")) }
