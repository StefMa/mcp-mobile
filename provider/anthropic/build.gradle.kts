plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.2.10"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
                implementation(libs.kotlinxSerializationJson)

                api(project(":provider"))
            }
        }
    }
}
