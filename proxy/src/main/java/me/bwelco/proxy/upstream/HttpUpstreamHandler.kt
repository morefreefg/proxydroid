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

class HttpUpstreamHandler(private val upstreamHandlerParam: UpstreamHandlerParam,
                          private val remoteProxyServer: InetAddress,
                          private val remoteProxyPort: Int) : UpstreamHandler(upstreamHandlerParam) {

    private val bootstrap: Bootstrap by lazy { Bootstrap() }
    private val remoteChannelClazz: Class<out Channel> by inject("remoteChannelClazz")

    override fun channelActive(ctx: ChannelHandlerContext) {
        val clientChannel = ctx.channel()

        bootstrap.group(clientChannel.eventLoop())
                .channel(remoteChannelClazz)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(LoggingHandler(LogLevel.INFO))
                .handler(ConnectHandler())

        bootstrap.connect(remoteProxyServer, remoteProxyPort).addFutureListener {
            ctx.channel().pipeline().remove(this)
            if (!it.isSuccess) {
                upstreamHandlerParam.remoteChannelPromise.
                        setFailure(Throwable("cannot achieve remote server ${remoteProxyServer}:${remoteProxyPort}"))
            }
        }
    }

    inner class ConnectHandler : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            val outboundChannel = ctx.channel()
            upstreamHandlerParam.remoteChannelPromise.setSuccess(outboundChannel)

            outboundChannel.pipeline().remove(this)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            upstreamHandlerParam.remoteChannelPromise.setFailure(cause)
        }
    }

}