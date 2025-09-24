pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.spring") version "2.2.20"
        id("org.springframework.boot") version "3.5.6"
        id("io.spring.dependency-management") version "1.1.7"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "timetracker"
