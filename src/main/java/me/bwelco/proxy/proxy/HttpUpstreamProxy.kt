package me.bwelco.proxy.proxy

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
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
            // start to relay data transparently

            outboundChannel.pipeline().addFirst("HttpResponseDecoder", HttpResponseDecoder())
            outboundChannel.pipeline().addAfter("HttpResponseDecoder", "HttpHandler", HttpHandler())
            outboundChannel.pipeline().addAfter("HttpHandler", "RelayHandler", RelayHandler(thisClientHandlerCtx.channel()))

//            outboundChannel.pipeline().addLast(RelayHandler(thisClientHandlerCtx.channel()))

            thisClientHandlerCtx.channel().pipeline().addLast(RelayHandler(outboundChannel))
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            promise.setFailure(cause)
        }
    }

    inner class HttpHandler: SimpleChannelInboundHandler<HttpResponse>(false) {
        override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpResponse) {
            if (msg.decoderResult().isSuccess) {
                println(msg.headers())
            }
            ctx.fireChannelRead(msg)
        }
    }

}