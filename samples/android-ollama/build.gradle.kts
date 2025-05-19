plugins {
    id("com.android.application") version "8.9.2"
    kotlin("android") version "2.1.20"
    kotlin("plugin.compose") version "2.1.20"
}

android {
    namespace = "guru.stefma.mcpmobile.android.sample"
    compileSdk = 35
    defaultConfig {
        targetSdk = 35
        applicationId = "guru.stefma.mcpmobile.android.sample"
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    kotlinOptions { jvmTarget = "1.8" }
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    implementation("guru.stefma.mcpmobile:mcpmobile:0.0.1-SNAPSHOT")
    implementation("guru.stefma.mcpmobile:provider-ollama:0.0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.compose.ui:ui:1.8.1")
    implementation("androidx.compose.material:material:1.8.1")
}
