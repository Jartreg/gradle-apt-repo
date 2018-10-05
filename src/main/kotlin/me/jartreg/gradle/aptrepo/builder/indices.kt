package me.jartreg.gradle.aptrepo.builder

import me.jartreg.gradle.aptrepo.Distribution
import me.jartreg.gradle.aptrepo.utils.FileInfo
import me.jartreg.gradle.aptrepo.utils.HashAlgorithm
import me.jartreg.gradle.aptrepo.utils.LineWriter
import me.jartreg.gradle.aptrepo.utils.writeFields
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal fun writePackageIndex(writer: LineWriter, packages: Iterable<PackagePool.Entry>) {
    var first = true

    for (pkg in packages) {
        if (!first)
            writer.writeLine()
        first = false

        val entry = pkg.packageEntry
        writeFields(entry.fields, writer)
        writeFields(mapOf(
                "Filename" to pkg.path,
                "Size" to entry.fileSize.toString()
        ), writer)
        writeFields(entry.checksums.mapKeys {
            when (it.key) {
                HashAlgorithm.MD5 -> "MD5sum"
                else -> it.key.toString()
            }
        }, writer)
    }
}

internal fun Distribution.writeReleaseFiles(writer: LineWriter, architectures: Iterable<String>, files: Iterable<FileInfo>, checksumAlgorithms: Array<HashAlgorithm>) {
    val fields = this.additionalFields.toMutableMap()
    fields += mapOf(
            "Origin" to this.origin.orEmpty(),
            "Label" to this.label.orEmpty(),
            "Suite" to this.suite,
            "Version" to this.version.orEmpty(),
            "Codename" to this.codename.orEmpty(),
            "Date" to DateTimeFormatter.RFC_1123_DATE_TIME.format(this.date.atOffset(ZoneOffset.UTC)),
            "Architectures" to architectures.joinToString(" "),
            "Components" to this.components.keys.joinToString(" "),
            "Description" to this.description.orEmpty()
    )

    writeFields(fields, writer)

    checksumAlgorithms.forEach { algorithm ->
        writer.writeLine(when (algorithm) {
            HashAlgorithm.MD5 -> "MD5Sum"
            else -> algorithm.toString()
        } + ":")

        files.forEach {
            writer.writeLine(" ${it.checksums[algorithm]} ${it.size} ${it.relativeName}")
        }
    }
}