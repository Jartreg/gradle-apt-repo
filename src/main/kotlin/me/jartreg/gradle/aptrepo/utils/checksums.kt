package me.jartreg.gradle.aptrepo.utils

import org.apache.commons.codec.binary.Hex
import org.bouncycastle.bcpg.HashAlgorithmTags
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

internal enum class HashAlgorithm(val algorithName: String, val algorithmTag: Int) {
    MD5("MD5", HashAlgorithmTags.MD5),
    SHA1("SHA-1", HashAlgorithmTags.SHA1),
    SHA256("SHA-256", HashAlgorithmTags.SHA256),
    SHA512("SHA-512", HashAlgorithmTags.SHA512);

    fun createMessageDigest() = MessageDigest.getInstance(algorithName)
}

internal fun getChecksums(stream: InputStream, algorithms: Array<HashAlgorithm>): Map<HashAlgorithm, String> {
    val digestMap = algorithms.associate { it to it.createMessageDigest() }
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    var readBytes = stream.read(buffer)
    while (readBytes != -1) {
        for (digest in digestMap.values) {
            digest.update(buffer, 0, readBytes)
        }

        readBytes = stream.read(buffer)
    }

    return digestMap.mapValues { Hex.encodeHexString(it.value.digest()) }
}

internal class MultiChecksumOutputStream(stream: OutputStream, algorithms: Array<HashAlgorithm>) : FilterOutputStream(stream) {
    private val digestMap = algorithms.associate { it to it.createMessageDigest() }

    val checksums
        get() = digestMap.mapValues { Hex.encodeHexString(it.value.digest()) }

    override fun write(b: Int) {
        super.write(b)
        digestMap.values.forEach { it.update(b.toByte()) }
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        super.write(b, off, len)
        digestMap.values.forEach { it.update(b, off, len) }
    }
}