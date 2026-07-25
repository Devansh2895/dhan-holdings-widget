plugins {
    // AGP 9 ships built-in Kotlin support, so the kotlin-android plugin is no longer applied.
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}
