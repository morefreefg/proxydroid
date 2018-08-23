package me.bwelco.proxy.http

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.FullHttpRequest
import me.bwelco.proxy.upstream.HttpsUpstream

class HostSelector(val interceptorMatcher: HttpInterceptorMatcher, val remoteHost: String) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is FullHttpRequest) {
            interceptorMatcher.match(remoteHost)?.apply {
                ctx.pipeline().addAfter(
                        "HostSelector",
                        "HttpInterceptorHandler",
                        MitmHandler(this))

                ctx.pipeline().addBefore("HttpInterceptorHandler", "CorrectCRLFHander", CorrectCRLFHander())
            }
        }

        ctx.fireChannelRead(msg)
    }
}