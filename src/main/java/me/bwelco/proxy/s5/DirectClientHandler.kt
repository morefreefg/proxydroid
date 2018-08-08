package me.bwelco.proxy.s5

import io.netty.channel.*
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.CustomNioSocketChannel
import java.net.Socket

class DirectClientHandler(val clientChannel: Channel,
                          val request: Socks5CommandRequest): ChannelInboundHandlerAdapter() {

    override fun channelRegistered(ctx: ChannelHandlerContext) {
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        val outboundChannel = ctx.channel()

        val responseFuture = clientChannel.writeAndFlush(DefaultSocks5CommandResponse(
                Socks5CommandStatus.SUCCESS,
                request.dstAddrType(),
                request.dstAddr(),
                request.dstPort()))

        responseFuture.addListener {
            outboundChannel.pipeline().addLast(RelayHandler(clientChannel))
            clientChannel.pipeline().addLast(RelayHandler(outboundChannel))
        }

        outboundChannel.pipeline().remove(this)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        clientChannel.writeAndFlush(
                DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()))
        SocksServerUtils.closeOnFlush(clientChannel)
    }
}