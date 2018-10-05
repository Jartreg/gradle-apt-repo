package me.jartreg.gradle.aptrepo.builder

import me.jartreg.gradle.aptrepo.utils.HashAlgorithm
import me.jartreg.gradle.aptrepo.utils.getChecksums
import me.jartreg.gradle.aptrepo.utils.parseFields
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.File
import java.io.InputStream
import java.util.concurrent.CompletableFuture

class DebPackageReadingException : Exception {
    constructor(file: File, reason: String) : super("Invalid packageEntry $file: $reason")
    constructor(file: File, cause: Throwable) : super("Exception while reading $file: ${cause.message}", cause)
}

internal data class PackageEntry(
        val name: String,
        val version: String,
        val architecture: String,
        val fields: Map<String, String>,
        val checksums: Map<HashAlgorithm, String>,
        val inputFile: File,
        val fileSize: Long
) {
    val outputFile = "${name}_${version}_$architecture.deb"
}

internal fun processPackage(file: File, checksumAlgorithms: Array<HashAlgorithm>): CompletableFuture<PackageEntry> {
    val descriptionFuture: CompletableFuture<Map<String, String>> = CompletableFuture.supplyAsync {
        getPackageFields(file)
    }

    val checksumsFuture: CompletableFuture<Map<HashAlgorithm, String>> = CompletableFuture.supplyAsync {
        file.inputStream().use {
            getChecksums(it, checksumAlgorithms)
        }
    }

    return descriptionFuture.thenCombine<Map<HashAlgorithm, String>, PackageEntry>(checksumsFuture) { description, checksums ->
        PackageEntry(
                description["Package"]
                        ?: throw DebPackageReadingException(file, "Missing Name field"),
                description["Version"]
                        ?: throw DebPackageReadingException(file, "Missing Version field"),
                description["Architecture"]
                        ?: throw DebPackageReadingException(file, "Missing Architecture field"),
                description,
                checksums,
                file,
                file.length()
        )
    }
}

private fun getPackageFields(file: File): Map<String, String> {
    file.inputStream().use {
        val stream = getControlFileFromPackage(it)
                ?: throw DebPackageReadingException(file, "Could not find the control file")

        try {
            stream.reader().buffered().use { reader ->
                return parseFields(reader)
            }
        } catch (ex: Exception) {
            throw DebPackageReadingException(file, ex)
        }
    }
}

private fun getControlFileFromPackage(stream: InputStream): InputStream? {
    val archiveStream = ArArchiveInputStream(stream)
    var entry: ArchiveEntry? = archiveStream.nextEntry
    while (entry != null) {
        if (!entry.isDirectory && entry.name.startsWith("control.tar")) {
            return getControlFileFromControlArchive(archiveStream)
        }

        entry = archiveStream.nextEntry
    }

    return null
}

val compressorStreamFactory = CompressorStreamFactory()
private fun getControlFileFromControlArchive(stream: InputStream): InputStream? {
    val archiveStream = TarArchiveInputStream(compressorStreamFactory.createCompressorInputStream(stream.buffered()))

    var entry: ArchiveEntry? = archiveStream.nextEntry
    while (entry != null) {
        if (!entry.isDirectory && entry.name == "./control") {
            return archiveStream
        }

        entry = archiveStream.nextEntry
    }

    return null
}