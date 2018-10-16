package me.bwelco.proxy.http

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpRequestDecoder

class HttpDetailHandler: ChannelInboundHandlerAdapter() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.pipeline().addLast("HttpDetailHandler_HttpRequestDecoder", HttpRequestDecoder())
        ctx.pipeline().addLast("HttpDetailHandler_HttpRequestHandler", HttpRequestHandler())
    }

    class HttpRequestHandler: ChannelInboundHandlerAdapter() {

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is HttpRequest) {
                val uri = msg.uri()

                val host = msg.headers()["Host"]
                ctx.pipeline().addLast("HttpRuleHandler", HttpRuleHandler(Remote(
                        host = host, port = 80, https = false, url = host + uri
                )))

                ctx.pipeline().remove("HttpDetailHandler_HttpRequestDecoder")
                ctx.pipeline().remove("HttpDetailHandler_HttpRequestHandler")

                ctx.fireChannelActive()
                ctx.fireChannelRead(msg)
            }

            super.channelRead(ctx, msg)
        }
    }

}