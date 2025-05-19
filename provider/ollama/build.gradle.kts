plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.1.21"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)

                api(project(":provider"))
                api(libs.kotlinxSerializationJson)
            }
        }
    }
}
