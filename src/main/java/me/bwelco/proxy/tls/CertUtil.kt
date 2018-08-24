package me.bwelco.proxy.tls

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

object CertUtil {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    val keyFactory: KeyFactory by lazy {
        KeyFactory.getInstance("RSA")
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun loadPriKey(bts: ByteArray): PrivateKey {
        val privateKeySpec = PKCS8EncodedKeySpec(bts)
        return keyFactory.generatePrivate(privateKeySpec)
    }

    @Throws(IOException::class, InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    fun loadPriKey(inputStream: InputStream): PrivateKey {
        val outputStream = ByteArrayOutputStream()
        val bts = ByteArray(1024)

        var len: Int
        while (true) {
            len = inputStream.read(bts)
            if (len == -1) {
                break
            }
            outputStream.write(bts, 0, len)
        }

        inputStream.close()
        outputStream.close()
        return loadPriKey(outputStream.toByteArray())
    }

    @Throws(Exception::class)
    fun loadPubKey(bts: ByteArray): PublicKey {
        val publicKeySpec = X509EncodedKeySpec(bts)
        return keyFactory.generatePublic(publicKeySpec)
    }

    @Throws(Exception::class)
    fun loadPubKey(inputStream: InputStream): PublicKey {
        val outputStream = ByteArrayOutputStream()
        val bts = ByteArray(1024)
        var len: Int
        while (true) {
            len = inputStream.read(bts)
            if (len == -1) {
                break
            }
            outputStream.write(bts, 0, len)
        }

        inputStream.close()
        outputStream.close()
        return loadPubKey(outputStream.toByteArray())
    }

    @Throws(CertificateException::class)
    fun loadCert(inputStream: InputStream): X509Certificate {
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(inputStream) as X509Certificate
    }

    @Throws(Exception::class)
    fun loadCert(path: String): X509Certificate {
        return loadCert(FileInputStream(path))
    }

    @Throws(Exception::class)
    fun genCert(issuer: String, caPriKey: PrivateKey, caNotBefore: Date,
                caNotAfter: Date, serverPubKey: PublicKey,
                vararg hosts: String): X509Certificate {

        val subject = "C=CN, ST=Zhejiang, L=Hangzhou, O=Youzan, OU=mobile, CN=" + hosts[0]
        val generalNames = hosts.map { GeneralName(GeneralName.dNSName, it) }.toTypedArray()

        val certHolder = JcaX509v3CertificateBuilder(X500Name(issuer),
                BigInteger.valueOf(System.currentTimeMillis() + (Math.random() * 10000).toLong() + 1000),
                caNotBefore,
                caNotAfter,
                X500Name(subject),
                serverPubKey)
                .addExtension(Extension.subjectAlternativeName, false, GeneralNames(generalNames))
                .build(JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey))

        return JcaX509CertificateConverter().getCertificate(certHolder)
    }

    @Throws(Exception::class)
    fun genCACert(subject: String,
                  caNotBefore: Date,
                  caNotAfter: Date,
                  keyPair: KeyPair): X509Certificate {

        val certHolder = JcaX509v3CertificateBuilder(
                X500Name(subject),
                BigInteger.valueOf(System.currentTimeMillis() + (Math.random() * 10000).toLong() + 1000),
                caNotBefore,
                caNotAfter,
                X500Name(subject),
                keyPair.public)
                .addExtension(Extension.basicConstraints, true, BasicConstraints(0))
                .build(JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.private))

        return JcaX509CertificateConverter().getCertificate(certHolder)
    }

    @Throws(Exception::class)
    fun genKeyPair(): KeyPair {
        val caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC")
        caKeyPairGen.initialize(1024, SecureRandom())
        return caKeyPairGen.genKeyPair()
    }

    @Throws(Exception::class)
    fun getSubject(inputStream: InputStream): String {
        val certificate = loadCert(inputStream)
        return certificate.issuerDN.toString()
                .split((", ").toRegex())
                .asReversed()
                .joinToString(separator = ", ") { it }
    }

    @Throws(Exception::class)
    fun getSubject(certificate: X509Certificate): String {
        return certificate.issuerDN.toString()
                .split((", ").toRegex())
                .asReversed()
                .joinToString(separator = ", ") { it }
    }

}