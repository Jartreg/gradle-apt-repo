package me.jartreg.gradle.aptrepo

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection

class Component(val name: String, project: Project) {
    val packages: ConfigurableFileCollection = project.files()

    fun include(packageFiles: Any) {
        packages.from(packageFiles)
    }
}