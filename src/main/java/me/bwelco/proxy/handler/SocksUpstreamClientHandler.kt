package me.bwelco.proxy.handler

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import me.bwelco.proxy.CustomNioSocketChannel
import me.bwelco.proxy.s5.SocksServerUtils
import me.bwelco.proxy.upstream.s5.SocksClientInitializer
import me.bwelco.proxy.util.addFutureListener
import java.net.Inet4Address

class SocksUpstreamClientHandler(val request: Socks5CommandRequest) : ChannelInboundHandlerAdapter() {

    val bootstrap: Bootstrap by lazy { Bootstrap() }
    private lateinit var thisClientHandlerCtx: ChannelHandlerContext

    val remoteSocks5Server = Inet4Address.getByName("58.20.41.172")
    val remoteSocks5ServerPort = 1080

    override fun channelActive(ctx: ChannelHandlerContext) {
        val clientChannel = ctx.channel()
        thisClientHandlerCtx = ctx

        bootstrap.group(clientChannel.eventLoop())
                .channel(CustomNioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(ConnectHandler())

        bootstrap.connect(remoteSocks5Server, remoteSocks5ServerPort).addFutureListener {
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
                    // start to relay by socks5 upstream
                    outboundChannel.pipeline().addLast(SocksClientInitializer(thisClientHandlerCtx.channel(), request))
                    outboundChannel.pipeline().fireChannelRegistered()
                    outboundChannel.pipeline().fireChannelActive()

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