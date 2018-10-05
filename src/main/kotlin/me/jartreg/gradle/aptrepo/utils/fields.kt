package me.jartreg.gradle.aptrepo.utils

import java.io.BufferedReader

class PropertyParsingException(message: String) : Exception(message)

internal fun parseFields(reader: BufferedReader): Map<String, String> {
    val properties = mutableMapOf<String, String>()

    var key = ""
    var value = ""
    var hasProperty = false

    for ((index, line) in reader.lineSequence().withIndex()) {
        if (line.length < 2) {
            break
        }

        val first = line.first()

        if (first == '#') {
            continue
        } else if (first.isWhitespace()) {
            if (!hasProperty) throw PropertyParsingException("Invalid whitespace at the beginning of line $index")
            value += "\n" + line.substring(1).trimEnd().takeIf { it != "." }.orEmpty()
        } else {
            if (!value.isEmpty()) {
                properties[key] = value
            }

            val colon = line.indexOf(':')
            if (colon == -1) throw PropertyParsingException("Missing colon on line $index")

            key = line.substring(0, colon).trimEnd()
            value = line.substring(colon + 1).trimStart()
            hasProperty = true
        }
    }

    if (!value.isEmpty()) {
        properties[key] = value.trim()
    }

    return properties
}

internal fun writeFields(properties: Map<String, String>, writer: LineWriter) {
    for ((key, value) in properties) {
        if (value.isEmpty())
            continue

        val lines = value.splitToSequence('\n').iterator()
        if (lines.hasNext())
            writer.writeLine("$key: ${lines.next()}")

        for (line in lines) {
            val field = line.trim().takeUnless { it.isEmpty() } ?: "."
            writer.writeLine(" $field")
        }
    }
}