package me.bwelco.proxy.handler

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import me.bwelco.proxy.CustomNioSocketChannel
import me.bwelco.proxy.s5.SocksServerUtils
import me.bwelco.proxy.upstream.s5.SocksClientInitializer
import me.bwelco.proxy.util.addFutureListener
import java.net.Inet4Address

class SocksUpstreamClientHandler(val request: Socks5CommandRequest) : ChannelInboundHandlerAdapter() {

    val bootstrap: Bootstrap by lazy { Bootstrap() }
    private lateinit var thisClientHandlerCtx: ChannelHandlerContext

    val remoteSocks5Server = Inet4Address.getByName("172.17.13.60")
    val remoteSocks5ServerPort = 6153

    override fun channelActive(ctx: ChannelHandlerContext) {
        val clientChannel = ctx.channel()
        thisClientHandlerCtx = ctx

        bootstrap.group(clientChannel.eventLoop())
                .channel(CustomNioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(LoggingHandler(LogLevel.INFO))
                .handler(ConnectHandler())

        bootstrap.connect(remoteSocks5Server, remoteSocks5ServerPort).addFutureListener {
            if (it.isSuccess) thisClientHandlerCtx.channel().pipeline().remove(this)
        }
    }

    inner class ConnectHandler : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {

            val outboundChannel = ctx.channel()

            outboundChannel.pipeline().addLast(SocksClientInitializer(thisClientHandlerCtx.channel(), request) { success ->
                if (success) {
                    thisClientHandlerCtx.channel().writeAndFlush(DefaultSocks5CommandResponse(
                            Socks5CommandStatus.SUCCESS,
                            request.dstAddrType(),
                            request.dstAddr(),
                            request.dstPort()))
                } else {
                    exceptionCaught(thisClientHandlerCtx, Throwable("remote fail"))
                }
            })

            outboundChannel.pipeline().remove(this)
            outboundChannel.pipeline().fireChannelActive()
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            thisClientHandlerCtx.channel().writeAndFlush(
                    DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()))
            SocksServerUtils.closeOnFlush(thisClientHandlerCtx.channel())
            SocksServerUtils.closeOnFlush(ctx.channel())
        }
    }
}