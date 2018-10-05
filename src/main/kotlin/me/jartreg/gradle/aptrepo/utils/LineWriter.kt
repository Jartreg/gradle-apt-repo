package me.jartreg.gradle.aptrepo.utils

import java.io.Closeable
import java.io.OutputStream
import java.nio.charset.Charset

internal interface LineWriter : Closeable {
    fun writeLine(line: String)
    fun writeLine()
}

internal fun OutputStream.lineWriter(charset: Charset = Charsets.UTF_8): LineWriter = OutputStreamLineWriter(this, charset)

private class OutputStreamLineWriter(private val stream: OutputStream, private val charset: Charset) : LineWriter {
    override fun close() {
        stream.close()
    }

    override fun writeLine(line: String) {
        stream.write("$line\n".toByteArray(charset))
    }

    override fun writeLine() {
        stream.write('\n'.toInt())
    }
}