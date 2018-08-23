package me.bwelco.proxy.http

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.AbstractSniHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.concurrent.Future
import me.bwelco.proxy.config.ProxyConfig
import me.bwelco.proxy.tls.SSLFactory
import me.bwelco.proxy.upstream.RelayHandler
import me.bwelco.proxy.util.isEmpty
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class SniHandler(val remoteChannel: Channel) : AbstractSniHandler<String>(), KoinComponent {

    val proxyConfig: ProxyConfig by inject()

    override fun onLookupComplete(ctx: ChannelHandlerContext, hostname: String, future: Future<String>) {
        if (hostname.isEmpty()) return

        val httpInterceptor = proxyConfig.mitmConfig().httpInterceptorMatcher.match(hostname)

        if (httpInterceptor == null) {
            ctx.pipeline().replace(this, "relayHandler", RelayHandler(remoteChannel))
            remoteChannel.pipeline().addLast(RelayHandler(ctx.channel()))
        } else {
            val downStreamTlsHandler = SslContextBuilder
                    .forServer(SSLFactory.certConfig.serverPrivateKey,
                            SSLFactory.newCert(hostname))
                    .build().newHandler(ctx.alloc())

            val upstreamTlsHandler = SslContextBuilder.forClient().build().newHandler(remoteChannel.alloc())

            // downstream
            ctx.pipeline().replace(this, "downStreamTlshandler", downStreamTlsHandler)
            remoteChannel.pipeline().addFirst("upstreamTlsHandler", upstreamTlsHandler)

            ctx.pipeline().addLast(HttpInterceptorInitializer(remoteChannel, httpInterceptor))
            ctx.pipeline().fireChannelActive()
            remoteChannel.pipeline().fireChannelActive()
        }
    }

    override fun lookup(ctx: ChannelHandlerContext, hostname: String): Future<String> {
        return ctx.executor().newPromise<String>().setSuccess("success")
    }
}