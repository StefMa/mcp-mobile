import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
    explicitApi()

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.modelcontextprotocol:kotlin-sdk:0.7.2")
                implementation(project(":provider"))
            }
        }
    }
}
