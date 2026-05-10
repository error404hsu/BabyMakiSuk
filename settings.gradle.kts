pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
include(":coremodel")
include(":coredata")
include(":coreui")
include(":coreai")
include(":featurehome")
include(":featuregrowth")
include(":featuremedical")
include(":featurevaccine")
include(":featurelog")
include(":featuresettings")
