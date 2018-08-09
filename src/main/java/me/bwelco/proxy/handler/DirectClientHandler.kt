package me.bwelco.proxy.handler

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import me.bwelco.proxy.CustomNioSocketChannel
import me.bwelco.proxy.s5.SocksServerUtils
import me.bwelco.proxy.util.addFutureListener

class DirectClientHandler(val request: Socks5CommandRequest) : ChannelInboundHandlerAdapter() {

    private val bootstrap: Bootstrap by lazy { Bootstrap() }
    private lateinit var thisClientHandlerCtx: ChannelHandlerContext

    override fun channelActive(ctx: ChannelHandlerContext) {
        val clientChannel = ctx.channel()
        thisClientHandlerCtx = ctx

        bootstrap.group(clientChannel.eventLoop())
                .channel(CustomNioSocketChannel::class.java)
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

            val responseFuture = thisClientHandlerCtx.channel().writeAndFlush(DefaultSocks5CommandResponse(
                    Socks5CommandStatus.SUCCESS,
                    request.dstAddrType(),
                    request.dstAddr(),
                    request.dstPort()))

            responseFuture.addFutureListener {
                if (it.isSuccess) {
                    outboundChannel.pipeline().remove(this)

                    // start to relay data transparently
                    outboundChannel.pipeline().addLast(RelayHandler(thisClientHandlerCtx.channel()))
                    thisClientHandlerCtx.channel().pipeline().addLast(RelayHandler(outboundChannel))
                } else {
                    exceptionCaught(thisClientHandlerCtx, it.cause())
                }
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            thisClientHandlerCtx.channel().writeAndFlush(
                    DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()))
            SocksServerUtils.closeOnFlush(thisClientHandlerCtx.channel())
            SocksServerUtils.closeOnFlush(ctx.channel())
        }
    }
}