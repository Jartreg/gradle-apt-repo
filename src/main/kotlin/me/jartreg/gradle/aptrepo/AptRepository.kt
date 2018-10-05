package me.jartreg.gradle.aptrepo

import me.jartreg.gradle.aptrepo.builder.RepositoryBuilder
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File

open class AptRepository : DefaultTask() {
    @Internal
    val distributions = mutableMapOf<String, Distribution>()

    @OutputDirectory
    var outputDir: File = File(project.buildDir, "apt-repository")

    @Nested
    @Optional
    var signingSpec: SigningSpec? = null

    val inputFiles: Iterable<Any>
        @InputFiles
        get() = distributions.values.asSequence()
                .flatMap { it.components.values.asSequence() }
                .map { it.packages }
                .asIterable()

    fun distribution(name: String) = distributions.computeIfAbsent(name) { Distribution(name, project) }

    fun distribution(name: String, configurationAction: Action<Distribution>): Distribution {
        return distribution(name).also(configurationAction::execute)
    }

    fun signing(configurationAction: Action<SigningSpec>) {
        val signing = signingSpec ?: SigningSpec(project).also {
            signingSpec = it
        }
        configurationAction.execute(signing)
    }

    @TaskAction
    fun buildRepository() {
        project.delete(outputDir)
        val builder = RepositoryBuilder(this, signingSpec?.createConfiguration())
        builder.buildRepository().get()
    }
}