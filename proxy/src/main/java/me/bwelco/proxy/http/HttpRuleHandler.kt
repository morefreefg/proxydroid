package me.bwelco.proxy.http

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.interceptor.Interceptor
import me.bwelco.proxy.interceptor.NetworkInterceptor
import me.bwelco.proxy.interceptor.RealInterceptorChain
import me.bwelco.proxy.proxy.DirectProxy
import me.bwelco.proxy.rule.ProxyRules
import me.bwelco.proxy.tls.SSLFactory
import me.bwelco.proxy.util.closeOnFlush
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject


class HttpRuleHandler(private val remote: Remote) : ChannelInboundHandlerAdapter(), KoinComponent {


    private val proxyConfig: ProxyRules by inject()

    override fun channelActive(ctx: ChannelHandlerContext) {
        val upstreamPromise: Promise<Channel> = ctx.executor().newPromise()

        upstreamPromise.addListener {
            if (it.isSuccess) {

            } else {
                ctx.channel().closeOnFlush()
            }
        }


        val httpInterceptor = proxyConfig.mitmConfig?.match(remote.host)

        if (httpInterceptor == null) {
            val downStreamTlsHandler = SslContextBuilder
                    .forServer(SSLFactory.certConfig.serverPrivateKey, SSLFactory.newCert(remote.host))
                    .build()
                    .newHandler(ctx.alloc())
//            val upstreamTlsHandler = SslContextBuilder.forClient().build().newHandler(remoteChannel.alloc())
//            ctx.pipeline().addLast(CorrectCRLFHander())

            ctx.pipeline().replace(this, "downStreamTlshandler", downStreamTlsHandler)
            ctx.pipeline().addLast(HttpResponseEncoder())
            ctx.pipeline().addLast(HttpRequestDecoder())
            ctx.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))

            ctx.pipeline().addLast("HttpMessageHandler", HttpMessageHandler())
        } else {

        }
    }

    inner class HttpMessageHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, request: Any) {
            if (request is FullHttpRequest) {

                val proxy = proxyConfig.proxylist[remote.host] ?: DirectProxy()
                val interceptors = listOf(NetworkInterceptor(request, proxy, remote))
                val realChain = RealInterceptorChain(
                        interceptors = interceptors,
                        index = 0,
                        request = request,
                        clientCtx = ctx)

                val response = realChain.proceed(request)
                ctx.writeAndFlush(response)

                if (!realChain.proceedBypassInternet) {
                    ctx.channel().closeOnFlush()
                }
            }

            ctx.fireChannelRead(request)
        }
    }


}