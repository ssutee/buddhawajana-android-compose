pluginManagement {
    includeBuild("build-logic")
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
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "Buddhawajana"
include(":app")
include(":core:model")
include(":core:common")
include(":core:network")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":core:player")
include(":feature:books")
