pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Tally"

includeBuild("build-logic")

include(":app")
include(":domain:core")
include(":domain:expenses")
include(":framework:database")
include(":framework:ai")
include(":feature:common")
include(":feature:resumen")
include(":test:unit")
