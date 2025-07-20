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

rootProject.name = "EasyCallAndAnswer"
include(":app")
/*include(":callsreportslibrary")*/
include(":sharedModules")
include(":lockscreen")
include(":voicerecognition")
include(":subscription")
include(":referrals")
include(":userengagement")
