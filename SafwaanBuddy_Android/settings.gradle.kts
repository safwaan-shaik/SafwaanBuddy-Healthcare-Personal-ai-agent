pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // For MPAndroidChart
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SafwaanBuddy Healthcare"
include(":app")