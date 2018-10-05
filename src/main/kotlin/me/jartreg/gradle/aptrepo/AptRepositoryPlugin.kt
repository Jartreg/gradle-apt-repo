package me.jartreg.gradle.aptrepo

import org.gradle.api.Plugin
import org.gradle.api.Project

class AptRepositoryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.extraProperties["AptRepository"] = AptRepository::class.java
    }
}