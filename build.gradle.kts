import org.gradle.internal.impldep.junit.runner.Version.id

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "ru.gildor.opublish"
version = "0.1.0"
description = "Gradle plugin that helps to configure Java and Android library publishing in opionated way"

gradlePlugin {
    plugins.invoke {
        "opublish" {
            id = project.group as String
            implementationClass = "ru.gildor.opublish.OpublishPlugin"
        }
    }
}

repositories {
    jcenter()
    gradlePluginPortal()
}

dependencies {
    compileOnly("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.7.3")
}