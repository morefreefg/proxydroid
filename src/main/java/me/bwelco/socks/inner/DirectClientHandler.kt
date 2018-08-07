package me.bwelco.socks.inner

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.concurrent.Promise
import me.bwelco.socks.CustomNioSocketChannel
import java.net.Socket

class DirectClientHandler(val promise: Promise<Channel>,
                          val connectListener: (Socket) -> Unit): ChannelInboundHandlerAdapter() {

    override fun channelRegistered(ctx: ChannelHandlerContext?) {
        if (ctx == null) return
        connectListener((ctx.channel() as CustomNioSocketChannel).rawSocket)
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        if (ctx == null) return
        ctx.pipeline().remove(this)
        promise.setSuccess(ctx.channel())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        promise.setFailure(cause)
    }
}