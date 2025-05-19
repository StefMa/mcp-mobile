plugins {
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinxSerializationJson)
            }
        }
    }
}

allprojects {
    plugins.apply("org.jetbrains.kotlin.multiplatform")

    kotlin {
        jvmToolchain(17)
        explicitApi()

        jvm()
    }
}

subprojects {
    pluginManager.apply("maven-publish")
    publishing {
        publications {
            // Unfortunately with afterEvaluate, otherwise we miss the artifactId for some targets...
            afterEvaluate {
                withType<MavenPublication> {
                    this.artifactId = artifactId.replace(project.name, "provider-${project.name}")
                }
            }
        }
    }
}