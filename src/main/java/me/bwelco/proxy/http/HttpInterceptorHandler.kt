package me.bwelco.proxy.http

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.FullHttpRequest

class HttpInterceptorHandler(val httpInterceptor: HttpInterceptor) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpRequest) {
            ctx.pipeline().addAfter("HttpInterceptorHandler", "MitmHandler", MitmHandler(httpInterceptor))
            ctx.pipeline().addBefore("MitmHandler", "CorrectCRLFHander", CorrectCRLFHander())
        }

        ctx.fireChannelRead(msg)
    }
}