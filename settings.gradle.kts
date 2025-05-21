plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "mcp-mobile"

include(
    "mcpmobile",
    "provider",
    "provider:anthropic",
    "provider:ollama",
    "provider:gemini",
)

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}
