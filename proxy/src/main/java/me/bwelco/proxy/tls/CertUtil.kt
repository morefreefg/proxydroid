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

    /**
     * How to generate ssl certificate
     * https://aboutssl.org/how-to-create-and-import-self-signed-certificate-to-android-device/
     *
     * echo basicConstraints=CA:true > android_options.txt
     * openssl genrsa -out ca_private.key 1024
     * openssl req -new -days 10680 -key ca_private.key -out ca.pem
     * openssl x509 -req -days 10680 -in ca.pem -signkey ca_private.key -extfile ./android_options.txt -out ca.crt
     * openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca_private.key -out ca_private.der
     *
     * generate android system ca
     * openssl x509 -inform PEM -subject_hash_old -in ca.crt | head -1
     * cat ca.crt > ${hash}.0
     * openssl x509 -inform PEM -text -in ca.crt -out /dev/null >> ${hash}.0
     *
     * adb push ${hash}.0 /sdcard
     * adb shell
     * mount -o remount,rw /system
     * su
     * cp -rf /sdacard/${hash}.0 /system/etc/security/cacerts/
     * chmod 644 /system/etc/security/cacerts/${hash}.0
     * reboot
     */
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private val keyFactory: KeyFactory by lazy {
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
    fun genCert(issuer: String, caPriKey: PrivateKey, serverPubKey: PublicKey,
                vararg hosts: String): X509Certificate {

        val subject = "C=CN, ST=zhejiang, L=hangzhou, O=youzan, OU=youzan, CN=" + hosts[0]
        val generalNames = hosts.map { GeneralName(GeneralName.dNSName, it) }.toTypedArray()

        val notBefore = Date()
        @Suppress("INTEGER_OVERFLOW")
        val norAfter = Date(notBefore.time + (12 * 30 * 24 * 60 * 60 * 1000).toLong())

        val certHolder = JcaX509v3CertificateBuilder(X500Name(issuer),
                BigInteger.valueOf(System.currentTimeMillis() + (Math.random() * 10000).toLong() + 1000),
                notBefore,
                norAfter,
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