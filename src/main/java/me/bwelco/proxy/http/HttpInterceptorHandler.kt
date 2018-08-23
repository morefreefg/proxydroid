package me.bwelco.proxy.http

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.FullHttpRequest
import me.bwelco.proxy.upstream.HttpsUpstream

class HttpInterceptorHandler(val httpInterceptor: HttpInterceptor) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpRequest) {
                ctx.pipeline().addAfter(
                        "HttpInterceptorHandler",
                        "HttpInterceptorHandler",
                        MitmHandler(httpInterceptor))

                ctx.pipeline().addBefore("HttpInterceptorHandler", "CorrectCRLFHander", CorrectCRLFHander())
        }

        ctx.fireChannelRead(msg)
    }
}