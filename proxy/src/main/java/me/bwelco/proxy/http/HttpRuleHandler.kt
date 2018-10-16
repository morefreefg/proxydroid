package me.bwelco.proxy.http

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5AddressType
import io.netty.handler.codec.socksx.v5.Socks5CommandType
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.interceptor.Interceptor
import me.bwelco.proxy.interceptor.NetworkInterceptor
import me.bwelco.proxy.interceptor.RealInterceptorChain
import me.bwelco.proxy.proxy.DirectProxy
import me.bwelco.proxy.rule.ProxyRules
import me.bwelco.proxy.tls.SSLFactory
import me.bwelco.proxy.upstream.UpstreamHandlerParam
import me.bwelco.proxy.util.closeOnFlush
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.Executors


class HttpRuleHandler(private val remote: Remote) : ChannelInboundHandlerAdapter(), KoinComponent {


    companion object {
        val remoteExcutor = Executors.newCachedThreadPool()!!
    }

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

        if (httpInterceptor != null) {
            if (remote.https) {
                val downStreamTlsHandler = SslContextBuilder
                        .forServer(SSLFactory.certConfig.serverPrivateKey, SSLFactory.newCert(remote.host))
                        .build()
                        .newHandler(ctx.alloc())
                ctx.pipeline().replace(this, "downStreamTlshandler", downStreamTlsHandler)
            }

//            val upstreamTlsHandler = SslContextBuilder.forClient().build().newHandler(remoteChannel.alloc())
//            ctx.pipeline().addLast(CorrectCRLFHander())

            ctx.pipeline().addLast(HttpResponseEncoder())
            ctx.pipeline().addLast(HttpRequestDecoder())
            ctx.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))

            ctx.pipeline().addLast("HttpMessageHandler", HttpMessageHandler())
        } else {
            val proxy = proxyConfig.proxylist[remote.host] ?: DirectProxy()
            val followUpAction = proxy.followUpAction()

            val remoteChannelPromise: Promise<Channel> = ctx.executor().newPromise<Channel>().addListener {
                if (it.isSuccess) {
                    val remoteChannel = it.now  as Channel

                } else {

                }
            }

            ctx.pipeline().addLast(proxy.createProxyHandler(UpstreamHandlerParam(
                    socks5Request = DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN,
                            remote.host, remote.port),
                    remoteChannelPromise = remoteChannelPromise
            )))

            ctx.pipeline().fireChannelActive()
        }
    }

    inner class HttpMessageHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, request: Any) {
            if (request is FullHttpRequest) {
                remoteExcutor.submit {
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
            }

            ctx.fireChannelRead(request)
        }
    }


}