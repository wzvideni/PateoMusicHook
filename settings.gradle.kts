pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
plugins {
    id("com.highcapable.sweetdependency") version "1.0.4"
    id("com.highcapable.sweetproperty") version "1.0.8"
}
sweetProperty {
    rootProject { all { isEnable = false } }
}
rootProject.name = "PateoMusicHook"
include(":app")
include(":traccarclient")
include(":traccarui")