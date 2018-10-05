package me.jartreg.gradle.aptrepo

import me.jartreg.gradle.aptrepo.builder.signing.SigningConfiguration
import me.jartreg.gradle.aptrepo.utils.HashAlgorithm
import me.jartreg.gradle.aptrepo.utils.getPrivateKey
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

class SigningSpec(private val project: Project) {
    @InputFile
    var keyRingFile: Any? = null

    @Input
    var keyId: String? = null

    @Input
    var passphrase: String? = null

    internal fun createConfiguration(): SigningConfiguration {
        val keyRingFile = project.file(keyRingFile ?: throw IllegalArgumentException("keyRingFile cannot be null"))
        val keyId = keyId?.toBigInteger(16)?.toLong() ?: throw IllegalArgumentException("keyId cannot be null")
        val passphrase = passphrase ?: throw IllegalArgumentException("passphrase cannot be null")

        val privateKey = getPrivateKey(keyRingFile, keyId, passphrase)
        return SigningConfiguration(privateKey, HashAlgorithm.SHA256)
    }
}
