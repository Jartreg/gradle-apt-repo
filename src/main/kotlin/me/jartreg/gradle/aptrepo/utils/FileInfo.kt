package me.jartreg.gradle.aptrepo.utils

import org.apache.commons.compress.utils.CountingOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream

internal data class FileInfo(
        val relativeName: String,
        val size: Long,
        val checksums: Map<HashAlgorithm, String>
)

internal class FileInfoBuilderStream(
        private val relativeName: String,
        checksumAlgorithms: Array<HashAlgorithm>,
        output: OutputStream
) : FilterOutputStream(null) {
    private val countingStream = CountingOutputStream(output)
    private val checksumStream = MultiChecksumOutputStream(countingStream, checksumAlgorithms)

    val fileInfo: FileInfo
        get() = FileInfo(
                relativeName,
                countingStream.bytesWritten,
                checksumStream.checksums
        )

    init {
        out = checksumStream
    }
}