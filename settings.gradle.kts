pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = "Piedone"

include(":app")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
