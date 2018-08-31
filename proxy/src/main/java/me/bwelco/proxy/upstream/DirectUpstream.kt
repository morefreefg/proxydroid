package me.bwelco.proxy.upstream

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.util.addFutureListener
import org.koin.standalone.inject
import java.rmi.Remote

class DirectUpstream(val request: Socks5CommandRequest,
                     val promise: Promise<Channel>) : Upstream(request, promise) {

    private val bootstrap: Bootstrap by lazy { Bootstrap() }
    private lateinit var thisClientHandlerCtx: ChannelHandlerContext
    private val remoteChannelClazz: Class<out Channel> by inject("remoteChannelClazz")

    override fun channelActive(ctx: ChannelHandlerContext) {
        val clientChannel = ctx.channel()
        thisClientHandlerCtx = ctx

        bootstrap.group(clientChannel.eventLoop())
                .channel(remoteChannelClazz)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(ConnectHandler())

        bootstrap.connect(request.dstAddr(), request.dstPort()).addFutureListener {
            if (it.isSuccess) thisClientHandlerCtx.channel().pipeline().remove(this)
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