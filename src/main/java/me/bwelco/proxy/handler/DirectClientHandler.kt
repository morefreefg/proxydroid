package me.bwelco.proxy.handler

import io.netty.channel.*
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus
import me.bwelco.proxy.s5.RelayHandler
import me.bwelco.proxy.s5.SocksServerUtils

class DirectClientHandler(val clientChannel: Channel): ChannelInboundHandlerAdapter() {

    override fun channelRegistered(ctx: ChannelHandlerContext) {
    }

    override fun channelActive(ctx: ChannelHandlerContext) {

        val outboundChannel = ctx.channel()

        outboundChannel.pipeline().addLast(RelayHandler(clientChannel))
        clientChannel.pipeline().addLast(RelayHandler(outboundChannel))

        outboundChannel.pipeline().remove(this)
    }

}