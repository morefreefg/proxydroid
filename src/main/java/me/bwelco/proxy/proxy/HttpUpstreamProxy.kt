package me.bwelco.proxy.proxy

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.logging.LoggingHandler
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.CustomNioSocketChannel
import me.bwelco.proxy.upstream.RelayHandler
import me.bwelco.proxy.util.addFutureListener

class HttpUpstreamProxy(val request: Socks5CommandRequest,
                        val promise: Promise<Channel>): ChannelInboundHandlerAdapter() {

    private val bootstrap: Bootstrap by lazy { Bootstrap() }
    private lateinit var thisClientHandlerCtx: ChannelHandlerContext


    override fun channelActive(ctx: ChannelHandlerContext) {
        val clientChannel = ctx.channel()
        thisClientHandlerCtx = ctx

        bootstrap.group(clientChannel.eventLoop())
                .channel(CustomNioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(ConnectHandler())

        bootstrap.connect(request.dstAddr(), request.dstPort()).addFutureListener {
            if (it.isSuccess) thisClientHandlerCtx.channel().pipeline().remove(this)
        }
    }

    inner class ConnectHandler : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            val outboundChannel = ctx.channel()
            promise.setSuccess(ctx.channel())

            outboundChannel.pipeline().remove(this)

            // hook http message
            val outerPipeline = outboundChannel.pipeline()

            outerPipeline.addLast(LoggingHandler())
            outerPipeline.addLast(HttpResponseDecoder())
            outerPipeline.addLast(HttpHandler())
            outerPipeline.addLast(RelayHandler(thisClientHandlerCtx.channel()))

            thisClientHandlerCtx.channel().pipeline().addLast(HttpResponseEncoder())
            thisClientHandlerCtx.channel().pipeline().addLast(RelayHandler(outboundChannel))

            outboundChannel.pipeline().fireChannelActive()
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            promise.setFailure(cause)
        }
    }

    inner class HttpHandler: ChannelInboundHandlerAdapter() {

        val message = java.lang.String("Fucking Silly Baidu").getBytes()

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is HttpResponse) {
                ctx.fireChannelRead(DefaultHttpResponse(msg.protocolVersion(),
                        msg.status(), msg.headers().set("Content-Type", "text/plain").set("Content-Length", message.size + 1)))
                return
            } else if (msg is HttpContent) {
                val hookedMessage = msg.copy() as DefaultLastHttpContent
                hookedMessage.content().clear()
                hookedMessage.content().writeBytes(message)
                hookedMessage.content().writeByte(0x0a)
                ctx.fireChannelRead(hookedMessage)
                return
            }

            ctx.fireChannelRead(msg)
        }
    }

}