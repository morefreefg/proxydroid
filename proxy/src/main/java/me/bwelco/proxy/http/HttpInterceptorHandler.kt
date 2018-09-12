package me.bwelco.proxy.http

import io.netty.channel.*
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.handler.logging.LoggingHandler
import me.bwelco.proxy.action.FollowUpAction
import me.bwelco.proxy.rule.ProxyRules
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class HttpInterceptorHandler(private val remoteChannel: Channel,
                             private val socks5Request: Socks5CommandRequest,
                             private val followUpAction: FollowUpAction): ChannelInboundHandlerAdapter() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.pipeline().addLast(HttpRequestDecoder())
        ctx.pipeline().addLast(HttpCheckHandler(remoteChannel, socks5Request, followUpAction))
        super.channelActive(ctx)
    }

    class HttpCheckHandler(val remoteChannel: Channel,
                           val socks5Request: Socks5CommandRequest,
                           val followUpAction: FollowUpAction)
        : SimpleChannelInboundHandler<HttpRequest>(), KoinComponent {

        val proxyConfig: ProxyRules by inject()

        override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
            if (msg.decoderResult().isSuccess) {
                val host = msg.headers()["host"] ?: socks5Request.dstAddr() ?: return
                val httpInterceptor = proxyConfig.mitmConfig?.match(host)

                if (httpInterceptor == null) {
                    try {
//                        ctx.pipeline().addLast(RelayHandler(remoteChannel))
//                        remoteChannel.pipeline().addLast(RelayHandler(ctx.channel()))
                        followUpAction.doFollowUp(ctx.channel(), remoteChannel)
                        remoteChannel.pipeline().addLast(HttpRequestEncoder())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    ctx.pipeline().addLast(MitmInitializer(remoteChannel, httpInterceptor, followUpAction))
                }
            } else {
                // not http, direct
//                ctx.pipeline().addLast(RelayHandler(remoteChannel))
//                remoteChannel.pipeline().addLast(RelayHandler(ctx.channel()))
                followUpAction.doFollowUp(ctx.channel(), remoteChannel)
            }

            ctx.pipeline().remove(this)
            ctx.pipeline().fireChannelRead(msg)
        }
    }


    class MitmInitializer(val remoteChannel: Channel,
                          val httpInterceptor: HttpInterceptor,
                          val followUpAction: FollowUpAction): ChannelInitializer<SocketChannel>() {
        override fun initChannel(clientChannel: SocketChannel) {
            clientChannel.pipeline().addLast(HttpResponseEncoder())
            clientChannel.pipeline().addLast(HttpRequestDecoder())
            clientChannel.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))

            clientChannel.pipeline().addLast("HttpMessageHandler", HttpMessageHandler(httpInterceptor))

//            clientChannel.pipeline().addLast(RelayHandler(remoteChannel))

            remoteChannel.pipeline().addLast(LoggingHandler())
            remoteChannel.pipeline().addLast(HttpResponseDecoder())
            remoteChannel.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))
//            remoteChannel.pipeline().addLast(RelayHandler(clientChannel))
            remoteChannel.pipeline().addLast(HttpRequestEncoder())

            followUpAction.doFollowUp(clientChannel, remoteChannel)
        }
    }


    class HttpMessageHandler(val httpInterceptor: HttpInterceptor): ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is FullHttpRequest) {
                ctx.pipeline().addAfter("HttpMessageHandler", "MitmHandler", MitmHandler(httpInterceptor))
                ctx.pipeline().addBefore("MitmHandler", "CorrectCRLFHander", CorrectCRLFHander())
            }

            ctx.fireChannelRead(msg)
        }
    }
}