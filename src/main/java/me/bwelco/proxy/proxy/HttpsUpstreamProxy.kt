package me.bwelco.proxy.proxy

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption
import io.netty.handler.codec.http.*
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.CustomNioSocketChannel
import me.bwelco.proxy.tls.SSLFactory
import me.bwelco.proxy.upstream.RelayHandler
import me.bwelco.proxy.util.addFutureListener

class HttpsUpstreamProxy(val request: Socks5CommandRequest,
                         val promise: Promise<Channel>) : ChannelInboundHandlerAdapter() {

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

        bootstrap.connect(request.dstAddr(), 80).addFutureListener {
            if (it.isSuccess) thisClientHandlerCtx.channel().pipeline().remove(this)
        }
    }

    inner class ConnectHandler : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            val outboundChannel = ctx.channel()
            promise.setSuccess(ctx.channel())

            outboundChannel.pipeline().remove(this)
            // start to relay data transparently

            val sslCtx = SslContextBuilder
                    .forServer(SSLFactory.certConfig.serverPrivateKey,
                            SSLFactory.newCert(request.dstAddr()))
                    .build()

            // hook https message
            val outerPipeline = outboundChannel.pipeline()


            outerPipeline.addLast(LoggingHandler())
            outerPipeline.addLast(HttpResponseDecoder())
            outerPipeline.addLast(HttpHandler())
            outerPipeline.addLast(RelayHandler(thisClientHandlerCtx.channel()))

            thisClientHandlerCtx.pipeline().addFirst("sslHandler", sslCtx.newHandler(ctx.alloc()))
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