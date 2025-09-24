import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.20" apply false
    id("org.springframework.boot") version "3.5.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "reai"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("io.spring.dependency-management")
        plugin("java")
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_24)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")

        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-webflux")
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
        "implementation"("org.springframework.boot:spring-boot-starter-thymeleaf")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")

        "implementation"("org.flywaydb:flyway-core:10.19.0")
        "implementation"("org.flywaydb:flyway-database-postgresql:10.19.0")
        "runtimeOnly"("org.postgresql:postgresql:42.7.4")

        "implementation"("io.jsonwebtoken:jjwt-api:0.11.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-impl:0.11.5")
        "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:0.11.5")
        "implementation"("com.auth0:java-jwt:4.4.0")

        "developmentOnly"("org.springframework.boot:spring-boot-devtools")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.testcontainers:junit-jupiter")
        "testImplementation"("org.testcontainers:postgresql")
        "testImplementation"("org.springframework.boot:spring-boot-testcontainers")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
