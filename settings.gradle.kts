/**
 * Configures the Gradle settings for the ScreenshotApp project.
 *
 * Inputs: None.
 * Outputs: Gradle configuration for plugin management and module inclusion.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ScreenshotApp"
include(":app")
