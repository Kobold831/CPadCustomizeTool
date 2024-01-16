pluginManagement {
    repositories {
        google()
        mavenCentral()
     }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "CPad Customize Tool"
include(":app")