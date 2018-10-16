package me.bwelco.proxy.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ssl.AbstractSniHandler
import io.netty.util.concurrent.Future
import me.bwelco.proxy.util.closeOnFlush
import me.bwelco.proxy.util.isEmpty
import org.koin.standalone.KoinComponent

class HttpsSniHandler : AbstractSniHandler<String>(), KoinComponent {

    override fun onLookupComplete(ctx: ChannelHandlerContext, hostname: String?, future: Future<String>) {
        if (hostname.isEmpty()) {
            ctx.channel().closeOnFlush()
        }

        ctx.pipeline().replace(this, "HttpRuleHandler",
                HttpRuleHandler(Remote(hostname!!, 443, true, hostname)))
        ctx.pipeline().fireChannelActive()

//        if (hostname.isEmpty()) return
//
//        val httpInterceptor = proxyConfig.mitmConfig?.match(hostname)
//
//        if (httpInterceptor == null) {
//            followUpAction.doFollowUp(ctx.channel(), remoteChannel)
//            ctx.pipeline().remove(this)
//        } else {
//            val downStreamTlsHandler = SslContextBuilder
//                    .forServer(SSLFactory.certConfig.serverPrivateKey,
//                            SSLFactory.newCert(hostname))
//                    .build()
//                    .newHandler(ctx.alloc())
//
//            val upstreamTlsHandler = SslContextBuilder.forClient().build().newHandler(remoteChannel.alloc())
//
//            // downstream
//            ctx.pipeline().replace(this, "downStreamTlshandler", downStreamTlsHandler)
//            remoteChannel.pipeline().addFirst("upstreamTlsHandler", upstreamTlsHandler)
//            ctx.pipeline().addLast(HttpInterceptorHandler(remoteChannel, socks5Request, followUpAction))
//        }
//
//        ctx.pipeline().fireChannelActive()
//        remoteChannel.pipeline().fireChannelActive()
    }

    override fun lookup(ctx: ChannelHandlerContext, hostname: String?): Future<String> {
        return ctx.executor().newPromise<String>().setSuccess("success")
    }

    /**
     * https check error
     */
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.channel().closeFuture()
//        ctx.pipeline().addLast(HttpInterceptorHandler(remoteChannel, socks5Request, followUpAction))
//        ctx.pipeline().fireChannelActive()
//        remoteChannel.pipeline().fireChannelActive()
    }
}