import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

group = "reai"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
        freeCompilerArgs.add("-Xannotation-default-target=param-property ")
    }
}

dependencies {
    "implementation"("org.jetbrains.kotlin:kotlin-reflect:2.2.20")
    "implementation"("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
    "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    "implementation"("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    "implementation"("org.springframework.boot:spring-boot-starter-web:3.5.6")
    "implementation"("org.springframework.boot:spring-boot-starter-webflux:3.5.6")
    "implementation"("org.springframework.boot:spring-boot-starter-data-jpa:3.5.6")
    "implementation"("org.springframework.boot:spring-boot-starter-thymeleaf:3.5.6")
    "implementation"("org.springframework.boot:spring-boot-starter-validation:3.5.6")
    "implementation"("org.springframework.boot:spring-boot-starter-actuator:3.5.6")

    "implementation"("org.flywaydb:flyway-core:10.19.0")
    "implementation"("org.flywaydb:flyway-database-postgresql:10.19.0")
    "runtimeOnly"("org.postgresql:postgresql:42.7.4")

    "implementation"("io.jsonwebtoken:jjwt-api:0.11.5")
    "runtimeOnly"("io.jsonwebtoken:jjwt-impl:0.11.5")
    "runtimeOnly"("io.jsonwebtoken:jjwt-jackson:0.11.5")
    "implementation"("com.auth0:java-jwt:4.4.0")

    "developmentOnly"("org.springframework.boot:spring-boot-devtools:3.5.6")

    "testImplementation"("org.springframework.boot:spring-boot-starter-test:3.5.6")
    "testImplementation"("org.testcontainers:junit-jupiter:1.20.2")
    "testImplementation"("org.testcontainers:postgresql:1.20.2")
    "testImplementation"("org.springframework.boot:spring-boot-testcontainers:3.5.6")
}

tasks.withType<Test> {
    useJUnitPlatform()
}