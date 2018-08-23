package me.bwelco.proxy.http

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.AbstractSniHandler
import io.netty.handler.ssl.SniHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.DomainNameMapping
import io.netty.util.DomainNameMappingBuilder
import io.netty.util.ReferenceCountUtil
import io.netty.util.concurrent.Future
import me.bwelco.proxy.config.ProxyConfig
import me.bwelco.proxy.proxy.DirectProxy
import me.bwelco.proxy.tls.SSLFactory
import me.bwelco.proxy.upstream.DirectUpstream
import me.bwelco.proxy.upstream.HttpsUpstream
import me.bwelco.proxy.upstream.RelayHandler
import me.bwelco.proxy.util.isEmpty
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class SniHandler(val remoteChannel: Channel) :  AbstractSniHandler<String>(), KoinComponent {

    val proxyConfig: ProxyConfig by inject()

    override fun onLookupComplete(ctx: ChannelHandlerContext, hostname: String, future: Future<String>) {
        if (hostname.isEmpty()) return

        if (proxyConfig.mitmConfig().httpInterceptorMatcher.match(hostname) == null) {
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
            ctx.pipeline().addLast(HttpResponseEncoder())
            ctx.pipeline().addLast(HttpRequestDecoder())
            ctx.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))
            ctx.pipeline().addLast("HostSelector",
                    HostSelector(proxyConfig.mitmConfig().httpInterceptorMatcher, hostname))
            ctx.pipeline().addLast(RelayHandler(remoteChannel))

            // upstream
            remoteChannel.pipeline().addFirst("upstreamTlsHandler", upstreamTlsHandler)
            remoteChannel.pipeline().addLast(LoggingHandler())
            remoteChannel.pipeline().addLast(HttpResponseDecoder())
            remoteChannel.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))
            remoteChannel.pipeline().addLast(RelayHandler(ctx.channel()))
            remoteChannel.pipeline().addLast(HttpRequestEncoder())

            remoteChannel.pipeline().fireChannelActive()
        }
    }

    override fun lookup(ctx: ChannelHandlerContext, hostname: String): Future<String> {
        return ctx.executor().newPromise<String>().setSuccess("success")
    }
}