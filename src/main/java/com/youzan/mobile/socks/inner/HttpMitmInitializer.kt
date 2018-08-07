package com.youzan.mobile.socks.inner

import com.youzan.mobile.socks.CustomNioSocketChannel
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.util.concurrent.Promise
import java.net.Socket

class HttpMitmInitializer(val promise: Promise<Channel>,
                          val connectListener: (Socket) -> Unit) : ChannelInboundHandlerAdapter() {

    override fun channelRegistered(ctx: ChannelHandlerContext?) {
        if (ctx == null) return
        connectListener((ctx.channel() as CustomNioSocketChannel).rawSocket)
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        if (ctx == null) return
        ctx.pipeline().remove(this)
        promise.setSuccess(ctx.channel())
    }

}