package me.bwelco.proxy.http

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.ssl.AbstractSniHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.concurrent.Future
import me.bwelco.proxy.rule.ProxyRules
import me.bwelco.proxy.tls.SSLFactory
import me.bwelco.proxy.upstream.RelayHandler
import me.bwelco.proxy.util.isEmpty
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class SniHandler(val remoteChannel: Channel,
                 val socks5Request: Socks5CommandRequest) : AbstractSniHandler<String>(), KoinComponent {

    val proxyConfig: ProxyRules by inject()

    override fun onLookupComplete(ctx: ChannelHandlerContext, hostname: String, future: Future<String>) {
        if (hostname.isEmpty()) return

        val httpInterceptor = proxyConfig.mitmConfig?.match(hostname)

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

            ctx.pipeline().addLast(HttpInterceptorHandler(remoteChannel, socks5Request))
            ctx.pipeline().fireChannelActive()
            remoteChannel.pipeline().fireChannelActive()
        }
    }

    override fun lookup(ctx: ChannelHandlerContext, hostname: String): Future<String> {
        return ctx.executor().newPromise<String>().setSuccess("success")
    }

    /**
     * tls check error
     */
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.pipeline().addLast(HttpInterceptorHandler(remoteChannel, socks5Request))
        ctx.pipeline().fireChannelActive()
        remoteChannel.pipeline().fireChannelActive()
    }
}