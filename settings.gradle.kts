pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.1.0"  // ✅ Match with build.gradle.kts
        id("com.google.gms.google-services") version "4.4.2"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT) // Prefer project-specific repos
    repositories {
        google()
        mavenCentral()
    }
}

// ✅ Set project name explicitly
rootProject.name = "fyp"

// ✅ Ensure app module is included
include(":app")
