package me.bwelco.proxy.upstream

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.codec.socksx.v5.*
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.util.addFutureListener
import org.koin.standalone.inject
import java.net.InetAddress

class Socks5Upstream(val request: Socks5CommandRequest,
                     val promise: Promise<Channel>,
                     val remoteSocks5Server: InetAddress,
                     val remoteSocks5ServerPort: Int) : Upstream(request, promise) {

    private val remoteChannelClazz: Class<out Channel> by inject("remoteChannelClazz")

    val bootstrap: Bootstrap by lazy { Bootstrap() }
    private lateinit var thisClientHandlerCtx: ChannelHandlerContext

    override fun channelActive(ctx: ChannelHandlerContext) {
        val clientChannel = ctx.channel()
        thisClientHandlerCtx = ctx

        bootstrap.group(clientChannel.eventLoop())
                .channel(remoteChannelClazz)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(LoggingHandler(LogLevel.INFO))
                .handler(ConnectHandler())

        bootstrap.connect(remoteSocks5Server, remoteSocks5ServerPort).addFutureListener {
            if (it.isSuccess)
                thisClientHandlerCtx.channel().pipeline().remove(this)
        }
    }

    inner class ConnectHandler : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {

            val outboundChannel = ctx.channel()
            outboundChannel.pipeline().addLast(SocksClientInitializer(thisClientHandlerCtx.channel(), request, promise))
            outboundChannel.pipeline().remove(this)
            outboundChannel.pipeline().fireChannelActive()
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            promise.setFailure(cause)
        }
    }

    class SocksClientInitializer(val downStreamChannel: Channel,
                                 val request: Socks5CommandRequest,
                                 val promise: Promise<Channel>) : ChannelInboundHandlerAdapter() {

        override fun channelActive(ctx: ChannelHandlerContext) {
            val inBoundChannel = ctx.channel()
            inBoundChannel.pipeline().addLast(Socks5ClientEncoder.DEFAULT)

            inBoundChannel.writeAndFlush(DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH)).addFutureListener { channelFuture ->
                channelFuture.channel().pipeline().remove(this)
                channelFuture.channel().pipeline().addLast(Socks5InitialResponseDecoder())
                channelFuture.channel().pipeline().addLast(Socks5InitialResponseHandler())
                channelFuture.channel().pipeline().fireChannelActive()
            }
        }

        inner class Socks5InitialResponseHandler : SimpleChannelInboundHandler<DefaultSocks5InitialResponse>() {

            override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5InitialResponse) {

                if (msg.decoderResult().isSuccess) {
                    // send command request
                    ctx.writeAndFlush(request).addFutureListener { channelFuture ->
                        if (channelFuture.isSuccess) {
                            ctx.pipeline().remove(this)
                            ctx.pipeline().addLast(Socks5CommandResponseDecoder())
                            ctx.pipeline().addLast(SocksCommandResponseHandler())
                        }
                    }
                }
            }
        }

        inner class SocksCommandResponseHandler : SimpleChannelInboundHandler<DefaultSocks5CommandResponse>() {

            override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5CommandResponse) {
                if (msg.decoderResult().isSuccess) {
                    promise.setSuccess(ctx.channel())
                    ctx.channel().pipeline().remove(this)
                }
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
            super.exceptionCaught(ctx, cause)
            promise.setFailure(cause)
        }
    }
}