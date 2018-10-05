package me.jartreg.gradle.aptrepo.builder

import me.jartreg.gradle.aptrepo.AptRepository
import me.jartreg.gradle.aptrepo.Component
import me.jartreg.gradle.aptrepo.Distribution
import me.jartreg.gradle.aptrepo.builder.signing.SigningConfiguration
import me.jartreg.gradle.aptrepo.builder.signing.SigningLineWriter
import me.jartreg.gradle.aptrepo.utils.*
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.CompletableFuture

internal class RepositoryBuilder(
        private val repository: AptRepository,
        private val signingConfiguration: SigningConfiguration? = null,
        private val checksumAlgorithms: Array<HashAlgorithm> = HashAlgorithm.values()
) {
    private val packageInputFiles = mutableMapOf<File, CompletableFuture<PackageEntry>>()
    private val packagePool = PackagePool()

    fun buildRepository(): CompletableFuture<Void> {
        val repositories = repository.distributions.values.map {
            buildDistribution(it)
        }

        return CompletableFuture.allOf(*repositories.toTypedArray()).thenAccept { _ ->
            val directory = repository.outputDir
            packagePool.contents.values.parallelStream()
                    .flatMap { it.values.parallelStream() }
                    .forEach {
                        val file = File(directory, it.path)
                        val parent = file.parentFile

                        if (!parent.isDirectory)
                            parent.mkdirs()

                        Files.copy(it.packageEntry.inputFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
        }
    }


    private fun buildDistribution(distribution: Distribution): CompletableFuture<Void> {
        val distDir = File(repository.outputDir, "dists/${distribution.name}")
        if (!distDir.isDirectory)
            distDir.mkdirs()

        val architectures = mutableSetOf<String>()
        val files = mutableListOf<FileInfo>()

        val componentFutures = distribution.components.values.map { component ->
            buildComponent(distDir, component).thenAccept {
                synchronized(architectures) { architectures += it.architectures }
                synchronized(files) { files += it.generatedFiles }
            }
        }

        return CompletableFuture.allOf(*componentFutures.toTypedArray()).thenAccept { _ ->
            createReleaseWriter(distDir).use {
                distribution.writeReleaseFiles(it, architectures, files, checksumAlgorithms)
            }
        }
    }

    private fun createReleaseWriter(distDir: File): LineWriter {
        val clearTextOutput = File(distDir, "Release").outputStream()

        return if (signingConfiguration != null) {
            SigningLineWriter(
                    clearTextOutput,
                    File(distDir, "Release.gpg").outputStream(),
                    File(distDir, "InRelease").outputStream(),
                    signingConfiguration
            )
        } else {
            clearTextOutput.lineWriter()
        }
    }

    private fun buildComponent(distDir: File, component: Component): CompletableFuture<BuiltComponent> {
        val packages = mutableMapOf<String, SortedMap<String, PackagePool.Entry>>()
        val packageFutures = component.packages.map { file ->
            getPackageEntry(file).thenAccept { pkg ->
                synchronized(packages) {
                    val map = packages.getOrPut(pkg.architecture) { sortedMapOf() }
                    if (map.containsKey(pkg.name)) throw Exception("Duplicate package ${pkg.name} for architecture ${pkg.architecture}")

                    map[pkg.name] = packagePool.addPackage(component, pkg)
                }
            }
        }

        return CompletableFuture.allOf(*packageFutures.toTypedArray()).thenCompose { _ ->
            val files = mutableListOf<FileInfo>()
            val fileFutures = packages.entries.map {
                CompletableFuture.runAsync {
                    files += createPackageFile(distDir, component.name, it.key, it.value)
                }
            }

            CompletableFuture.allOf(*fileFutures.toTypedArray()).thenApply {
                BuiltComponent(
                        packages.keys,
                        files
                )
            }
        }
    }

    private fun createPackageFile(distDir: File, componentName: String, architecture: String, packages: Map<String, PackagePool.Entry>): List<FileInfo> {
        val uncompressedName = "$componentName/binary-$architecture/Packages"
        val compressedName = "$uncompressedName.xz"

        val file = File(distDir, compressedName)
        val parent = file.parentFile
        if (!parent.isDirectory)
            parent.mkdirs()

        val compressedOutput = FileInfoBuilderStream(compressedName, checksumAlgorithms, file.outputStream())
        val uncompressedOutput = FileInfoBuilderStream(uncompressedName, checksumAlgorithms, XZCompressorOutputStream(compressedOutput))
        uncompressedOutput.lineWriter().use {
            writePackageIndex(it, packages.values)
        }

        return listOf(
                compressedOutput.fileInfo,
                uncompressedOutput.fileInfo
        )
    }

    private fun getPackageEntry(packageFile: File) = packageInputFiles.getOrPut(packageFile) {
        processPackage(packageFile, checksumAlgorithms)
    }

    private data class BuiltComponent(
            val architectures: Set<String>,
            val generatedFiles: List<FileInfo>
    )
}
