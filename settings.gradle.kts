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

rootProject.name = "NotesAppSync"

// Modulo de entrada
include(":app")

// Capa core
include(":core:domain")
include(":core:ui")
include(":core:database")
include(":core:network")
include(":core:sync")

// Capa feature
include(":feature:notes")
