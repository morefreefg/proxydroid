package me.bwelco.proxy.http

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import me.bwelco.proxy.util.addFutureListener
import me.bwelco.proxy.util.closeOnFlush

class MitmHandler(val httpInterceptor: HttpInterceptor) : ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpRequest) {
            val newRequest = httpInterceptor.onRequest(msg)
            ctx.fireChannelRead(newRequest)
        } else {
            ctx.fireChannelRead(msg)
        }
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if (msg is FullHttpResponse) {
            val newResponse = httpInterceptor.onResponse(msg)
            ctx.writeAndFlush(newResponse).addFutureListener {
                if (it.isSuccess) {
                    ctx.channel().closeOnFlush()
                }
            }
        } else {
            ctx.write(msg, promise)
        }
    }
}