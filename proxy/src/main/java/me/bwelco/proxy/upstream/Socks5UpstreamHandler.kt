package me.bwelco.proxy.upstream

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.codec.socksx.v5.*
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.util.addFutureListener
import me.bwelco.proxy.util.isEmpty
import org.koin.standalone.inject
import java.net.InetAddress

class Socks5UpstreamHandler(val request: Socks5CommandRequest,
                            val promise: Promise<Channel>,
                            val remoteSocks5Server: InetAddress,
                            val remoteSocks5ServerPort: Int,
                            val userName: String?,
                            val passwd: String?) : UpstreamHandler(request, promise) {

    private val remoteChannelClazz: Class<out Channel> by inject("remoteChannelClazz")

    private val bootstrap: Bootstrap by lazy { Bootstrap() }
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

    inner class SocksClientInitializer(val downStreamChannel: Channel,
                                       val request: Socks5CommandRequest,
                                       val promise: Promise<Channel>) : ChannelInboundHandlerAdapter() {

        override fun channelActive(ctx: ChannelHandlerContext) {
            val inBoundChannel = ctx.channel()
            inBoundChannel.pipeline().addLast(Socks5ClientEncoder.DEFAULT)

            inBoundChannel.writeAndFlush(DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH, Socks5AuthMethod.PASSWORD))
                    .addFutureListener { channelFuture ->
                        channelFuture.channel().pipeline().remove(this)
                        channelFuture.channel().pipeline().addLast(Socks5InitialResponseDecoder())
                        channelFuture.channel().pipeline().addLast(Socks5InitialResponseHandler())
                        channelFuture.channel().pipeline().fireChannelActive()
                    }
        }

        inner class Socks5PasswordAuthResponseHandler : SimpleChannelInboundHandler<Socks5PasswordAuthResponse>() {
            override fun channelRead0(ctx: ChannelHandlerContext, msg: Socks5PasswordAuthResponse) {
                if (msg.decoderResult().isSuccess && msg.status() == Socks5PasswordAuthStatus.SUCCESS) {
                    ctx.writeAndFlush(request).addFutureListener { channelFuture ->
                        if (channelFuture.isSuccess) {
                            ctx.pipeline().remove(this)
                            ctx.pipeline().addLast(Socks5CommandResponseDecoder())
                            ctx.pipeline().addLast(SocksCommandResponseHandler())
                        }
                    }
                } else {
                    promise.setFailure(Throwable("Socks5PasswordAuthResponseHandler: decode fail or password fail"))
                }
            }
        }

        inner class Socks5InitialResponseHandler : SimpleChannelInboundHandler<DefaultSocks5InitialResponse>() {
            override fun channelRead0(ctx: ChannelHandlerContext, msg: DefaultSocks5InitialResponse) {
                if (msg.decoderResult().isSuccess) {
                    when (msg.authMethod()) {
                        Socks5AuthMethod.NO_AUTH -> {
                            ctx.writeAndFlush(request).addFutureListener { channelFuture ->
                                if (channelFuture.isSuccess) {
                                    ctx.pipeline().remove(this)
                                    ctx.pipeline().addLast(Socks5CommandResponseDecoder())
                                    ctx.pipeline().addLast(SocksCommandResponseHandler())
                                }
                            }
                        }

                        Socks5AuthMethod.PASSWORD -> {
                            ctx.pipeline().remove(this)
                            ctx.pipeline().addLast(Socks5PasswordAuthResponseDecoder())
                            ctx.pipeline().addLast(Socks5PasswordAuthResponseHandler())

                            ctx.writeAndFlush(DefaultSocks5PasswordAuthRequest(userName, passwd))
                        }
                    }
                } else {
                    promise.setFailure(Throwable("Socks5InitialResponseHandler: decode fail"))
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