// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply(false)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

buildscript {
    extra.apply {
        set("GITHUB_USER", project.findProperty("GITHUB_USER") as String? ?: System.getenv("GITHUB_USER"))
        set("GITHUB_PAT", project.findProperty("GITHUB_PAT") as String? ?: System.getenv("GITHUB_PAT"))
    }

    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/getzoop/zoop-package-public")
            credentials {
                username = rootProject.extra.get("GITHUB_USER") as String
                password = rootProject.extra.get("GITHUB_PAT") as String
            }
        }
        flatDir {
            dirs = setOf(File("$rootDir/ext_libs"))
        }
    }
}