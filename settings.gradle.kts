pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BabyMakiSuk"

include(":app")

// Core modules
include(":core:model")
include(":core:data")
include(":core:ui")
include(":core:ai")

// Feature modules
include(":feature:home")
include(":feature:growth")
include(":feature:medical")
include(":feature:vaccine")
include(":feature:log")
include(":feature:settings")
