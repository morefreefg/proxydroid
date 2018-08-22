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

class CustomSniHandler(val mapping: DomainNameMapping<SslContext>) : SniHandler(mapping), KoinComponent {

    val proxyConfig: ProxyConfig by inject()

    override fun replaceHandler(ctx: ChannelHandlerContext, hostname: String?, fakedSslContext: SslContext) {
//        super.replaceHandler(ctx, hostname, sslContext)

//        var sslHandler: SslHandler? = null
//        try {
//            sslHandler = sslContext.newHandler(ctx.alloc())
//            ctx.pipeline().replace(this, SslHandler::class.java.name, sslHandler)
//            sslHandler = null
//        } finally {
//            // Since the SslHandler was not inserted into the pipeline the ownership of the SSLEngine was not
//            // transferred to the SslHandler.
//            // See https://github.com/netty/netty/issues/5678
//            if (sslHandler != null) {
//                ReferenceCountUtil.safeRelease(sslHandler.engine())
//            }
//        }
//

        if (hostname.isEmpty()) return

        if (proxyConfig.mitmConfig().httpInterceptorMatcher.match(hostname!!) == null) {
            super.replaceHandler(ctx, hostname, fakedSslContext)
        }

        val downStreamSSLContext = SslContextBuilder
                .forServer(SSLFactory.certConfig.serverPrivateKey,
                        SSLFactory.newCert(request.dstAddr()))
                .build()



    }
}