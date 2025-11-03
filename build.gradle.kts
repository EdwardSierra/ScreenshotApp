/**
 * Defines the top-level Gradle build configuration including plugin versions.
 *
 * Inputs: None.
 * Outputs: Plugin configuration applied to sub-modules.
 */
plugins {
    id("com.android.application") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
