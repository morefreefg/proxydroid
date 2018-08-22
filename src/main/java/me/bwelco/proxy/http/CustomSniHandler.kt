package me.bwelco.proxy.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ssl.SniHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.util.DomainNameMapping
import io.netty.util.ReferenceCountUtil
import me.bwelco.proxy.config.ProxyConfig
import me.bwelco.proxy.tls.SSLFactory
import me.bwelco.proxy.util.isEmpty
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class CustomSniHandler(mapping: DomainNameMapping<SslContext>) : SniHandler(mapping), KoinComponent {

    val proxyConfig: ProxyConfig by inject()

    override fun replaceHandler(ctx: ChannelHandlerContext, hostname: String?, fakedSslContext: SslContext) {

        if (hostname.isEmpty()) return

        if (proxyConfig.mitmConfig().httpInterceptorMatcher.match(hostname!!) == null) {
            return super.replaceHandler(ctx, hostname, fakedSslContext)
        }

        val downStreamSSLContext = SslContextBuilder
                .forServer(SSLFactory.certConfig.serverPrivateKey,
                        SSLFactory.newCert(hostname))
                .build()
        return super.replaceHandler(ctx, hostname, downStreamSSLContext)
    }
}