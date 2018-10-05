package me.jartreg.gradle.aptrepo.builder

import me.jartreg.gradle.aptrepo.Component

internal class PackagePool {
    private val components = mutableMapOf<String, MutableMap<String, Entry>>()

    val contents: Map<String, Map<String, Entry>>
        get() = components

    fun addPackage(component: Component, pkg: PackageEntry): Entry {
        val componentMap = components.getOrPut(component.name) { mutableMapOf() }
        val file = pkg.outputFile

        val existing = componentMap[file]
        if (existing != null && existing.packageEntry.inputFile != pkg.inputFile) {
            throw Exception("Multiple input files for $file in component ${component.name}: ${existing.packageEntry.inputFile} and ${pkg.inputFile}")
        }

        val entry = Entry(
                pkg,
                "pool/${component.name}/${pkg.name.first()}/${pkg.name}/$file"
        )
        componentMap[file] = entry
        return entry
    }

    data class Entry(
            val packageEntry: PackageEntry,
            val path: String
    )
}