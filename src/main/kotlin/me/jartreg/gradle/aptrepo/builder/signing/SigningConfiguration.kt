package me.jartreg.gradle.aptrepo.builder.signing

import me.jartreg.gradle.aptrepo.utils.HashAlgorithm
import org.bouncycastle.openpgp.PGPPrivateKey
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder

internal class SigningConfiguration(
        private val privateKey: PGPPrivateKey,
        val hashAlgorithm: HashAlgorithm
) {
    private val contentSignerBuilder = BcPGPContentSignerBuilder(privateKey.publicKeyPacket.algorithm, hashAlgorithm.algorithmTag)

    fun createSignatureGenerator() = PGPSignatureGenerator(contentSignerBuilder).also {
        it.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey)
    }
}