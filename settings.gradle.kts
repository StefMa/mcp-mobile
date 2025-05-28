plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mcp-mobile"

include(
    "mcpmobile",
    "provider",
    "provider:anthropic",
    "provider:ollama",
    "provider:openai",
)

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}