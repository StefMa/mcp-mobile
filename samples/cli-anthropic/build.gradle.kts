plugins {
    kotlin("jvm") version "2.1.20"
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("guru.stefma.mcpmobile:mcpmobile:0.0.1-SNAPSHOT")
    implementation("guru.stefma.mcpmobile:provider-anthropic:0.0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

application {
    mainClass.set("MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
