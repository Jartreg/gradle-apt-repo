package me.jartreg.gradle.aptrepo.utils

import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.bouncycastle.openpgp.PGPUtil
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import java.io.File

fun getSecretKeyRingCollection(file: File) = file.inputStream().use { stream ->
    PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(stream), JcaKeyFingerprintCalculator())
}

fun PGPSecretKey.extractPrivateKey(password: String): PGPPrivateKey =
        this.extractPrivateKey(JcePBESecretKeyDecryptorBuilder().build(password.toCharArray()))

fun getPrivateKey(keyRingFile: File, keyId: Long, passphrase: String): PGPPrivateKey {
    val keyring = getSecretKeyRingCollection(keyRingFile)
    val secretKey = keyring.getSecretKey(keyId)
            ?: throw Exception("Could not find key ${keyId.toString(16)} in file ${keyRingFile.path}")
    return secretKey.extractPrivateKey(passphrase)
}