plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.anthropic)

                api(project(":provider"))
            }
        }
    }
}
