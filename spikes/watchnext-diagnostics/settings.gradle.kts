// The Watch Next diagnostics app: a report on the TV provider's Watch Next and channel items and whether their
// pictures open, to find why some apps' pictures do not show in MyTVLauncher. A build of its own, outside the app's;
// from the repository's root: `./gradlew -p spikes/watchnext-diagnostics assembleDebug`.
pluginManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}

rootProject.name = "WatchNextDiagnostics"
include(":app")
