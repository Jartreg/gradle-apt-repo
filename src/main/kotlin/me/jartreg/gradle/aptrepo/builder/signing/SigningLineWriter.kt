package me.jartreg.gradle.aptrepo.builder.signing

import me.jartreg.gradle.aptrepo.utils.LineWriter
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.BCPGOutputStream
import java.io.OutputStream

internal class SigningLineWriter(
        private val clearTextOutput: OutputStream,
        private val detachedOutput: OutputStream,
        attachedOutput: OutputStream,
        signingConfiguration: SigningConfiguration
) : LineWriter {
    private var closed = false
    private var firstLine = true

    private val attachedGenerator = signingConfiguration.createSignatureGenerator()
    private val detachedGenerator = signingConfiguration.createSignatureGenerator()

    private val attachedOutput = ArmoredOutputStream(attachedOutput).also {
        it.beginClearText(signingConfiguration.hashAlgorithm.algorithmTag)
    }

    override fun writeLine(line: String) {
        writeLine()

        val rawLine = line.toByteArray()
        detachedGenerator.update(rawLine)
        clearTextOutput.write(rawLine)

        val trimmedLine = line.trimEnd().toByteArray()
        attachedGenerator.update(trimmedLine)
        attachedOutput.write(trimmedLine)
    }

    override fun writeLine() {
        detachedGenerator.update('\n'.toByte())
        clearTextOutput.write('\n'.toInt())

        if (!firstLine) {
            attachedGenerator.update('\n'.toByte())
            attachedOutput.write('\n'.toInt())
        }
        firstLine = false
    }

    override fun close() {
        if (closed)
            throw IllegalStateException("already closed")
        closed = true

        attachedOutput.write('\n'.toInt())
        attachedOutput.endClearText()
        BCPGOutputStream(attachedOutput).use {
            attachedGenerator.generate().encode(it)
        }

        BCPGOutputStream(ArmoredOutputStream(detachedOutput)).use {
            detachedGenerator.generate().encode(it)
        }

        clearTextOutput.close()
    }
}