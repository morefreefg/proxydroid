package me.bwelco.proxy.interceptor


import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandRequest
import io.netty.handler.codec.socksx.v5.Socks5AddressType
import io.netty.handler.codec.socksx.v5.Socks5CommandType
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.http.Remote
import me.bwelco.proxy.proxy.Proxy
import me.bwelco.proxy.upstream.UpstreamHandlerParam
import me.bwelco.proxy.util.closeOnFlush


internal class NetworkInterceptor(private val request: FullHttpRequest,
                                  private val proxy: Proxy,
                                  private val remote: Remote) : Interceptor {


    var response: FullHttpResponse? = null
    val lock = java.lang.Object()

    override fun intercept(chain: Interceptor.Chain): FullHttpResponse {

        synchronized(lock) {
            val clientCtx = chain.clientContext()

            val remoteChannelPromise: Promise<Channel> = clientCtx.executor().newPromise<Channel>().addListener {
                if (it.isSuccess) {
                    val remoteChannel = it.now as Channel
                    if (remote.https) {
                        val upstreamTlsHandler = SslContextBuilder.forClient().build().newHandler(remoteChannel.alloc())
                        remoteChannel.pipeline().addFirst("upstreamTlsHandler", upstreamTlsHandler)
                        remoteChannel.pipeline().addLast(HttpResponseDecoder())
//                        remoteChannel.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))
                        remoteChannel.pipeline().addLast(HttpMessageHandler())

                        remoteChannel.pipeline().addAfter(
                                "upstreamTlsHandler",
                                "HttpRequestEncoder",
                                HttpRequestEncoder())

                        remoteChannel.pipeline().write(request)
                    }
                } else {
                    clientCtx.channel().closeOnFlush()
                }
            }

            val proxyHandler = proxy.createProxyHandler(UpstreamHandlerParam(
                    socks5Request = DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN,
                            remote.host, remote.port),
                    remoteChannelPromise = remoteChannelPromise
            ))

            clientCtx.pipeline().addLast(proxyHandler)
            clientCtx.pipeline().fireChannelActive()

            lock.wait()

        }
        return response!!
    }

    inner class HttpMessageHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            println(msg)
//            if (msg is FullHttpResponse) {
//                this@NetworkInterceptor.response = msg
//                synchronized(lock) {
//                    lock.notifyAll()
//                }
//            }
        }
    }
}