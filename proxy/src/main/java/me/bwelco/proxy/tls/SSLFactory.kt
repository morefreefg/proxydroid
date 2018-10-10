package me.bwelco.proxy.tls

import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object SSLFactory {
    var certConfig: CertificateConfig

    data class CertificateConfig(
            //读取CA证书使用者信息
            val iUser: String,
            //CA私钥用于给动态生成的网站SSL证书签证
            val caPriKey: PrivateKey,
            //生产一对随机公私钥用于网站SSL证书动态创建
            val serverPublicKey: PublicKey,
            val serverPrivateKey: PrivateKey)

    private val certCache = ConcurrentHashMap<String, X509Certificate>()

    fun newCert(host: String): X509Certificate {
        val key = host.trim().toLowerCase()

        return certCache.get(key) ?: CertUtil.genCert(certConfig.iUser,
                certConfig.caPriKey,
                certConfig.serverPublicKey,
                key).apply { certCache.put(key, this) }
    }

    init {
        val classLoader = Thread.currentThread().contextClassLoader
        val caCert = CertUtil.loadCert(classLoader.getResourceAsStream("ca.crt"))
        val caPriKey = CertUtil.loadPriKey(classLoader.getResourceAsStream("ca_private.der"))

        val keyPair = CertUtil.genKeyPair()


        certConfig = CertificateConfig(iUser = CertUtil.getSubject(caCert),
                caPriKey = caPriKey,
                serverPrivateKey = keyPair.getPrivate(),
                serverPublicKey = keyPair.getPublic())
    }

    fun preloadCertificate(hostList: List<String>) {
        Thread { hostList.forEach { newCert(it) } }.start()
    }
}