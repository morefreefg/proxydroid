package me.bwelco.proxy.upstream

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.util.addFutureListener
import org.koin.standalone.inject
import java.net.InetAddress

class HttpUpstream(val request: Socks5CommandRequest,
                   val promise: Promise<Channel>,
                   val remoteProxyServer: InetAddress,
                   val remoteProxyPort: Int) : Upstream(request, promise) {

    val bootstrap: Bootstrap by lazy { Bootstrap() }
    private lateinit var thisClientHandlerCtx: ChannelHandlerContext
    private val remoteChannelClazz: Class<out Channel> by inject("remoteChannelClazz")


    override fun channelActive(ctx: ChannelHandlerContext) {
        val clientChannel = ctx.channel()
        thisClientHandlerCtx = ctx

        bootstrap.group(clientChannel.eventLoop())
                .channel(remoteChannelClazz)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(LoggingHandler(LogLevel.INFO))
                .handler(ConnectHandler())

        bootstrap.connect(remoteProxyServer, remoteProxyPort).addFutureListener {
            if (it.isSuccess)
                thisClientHandlerCtx.channel().pipeline().remove(this)
        }
    }

    inner class ConnectHandler : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            val outboundChannel = ctx.channel()
            promise.setSuccess(outboundChannel)

            outboundChannel.pipeline().remove(this)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            promise.setFailure(cause)
        }
    }

}