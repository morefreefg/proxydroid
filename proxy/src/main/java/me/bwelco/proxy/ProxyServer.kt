package me.bwelco.proxy

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import me.bwelco.proxy.config.Config
import me.bwelco.proxy.config.HttpInterceptorConfig
import me.bwelco.proxy.config.ProxyConfig
import me.bwelco.proxy.downstream.SocksServerInitializer
import me.bwelco.proxy.http.HttpInterceptor
import me.bwelco.proxy.http.HttpInterceptorMatcher
import me.bwelco.proxy.proxy.Proxy
import me.bwelco.proxy.proxy.Socks5Proxy
import me.bwelco.proxy.proxy.UpstreamMatchHandler
import org.apache.commons.logging.LogFactory
import org.koin.dsl.module.Module
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext.closeKoin
import org.koin.standalone.StandAloneContext.startKoin

fun main(args: Array<String>) {
    ProxyServer.start(1080, NioSocketChannel::class.java)
}

class BaiduHttpInterceptor : HttpInterceptor {

    override fun onRequest(request: FullHttpRequest): FullHttpRequest {
        return request
    }

    override fun onResponse(response: FullHttpResponse): FullHttpResponse {
        return response.replace(Unpooled.wrappedBuffer(java.lang.String(
                "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "\t<title>fucking baidu</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "\t<h1>hooked html</h1>\n" +
                        "\n" +
                        "</body>\n" +
                        "</html>").getBytes())).apply {
            this.headers().remove("Content-Encoding")
        }
    }
}

object ProxyServer {

    val logger = LogFactory.getLog(ProxyServer::class.java)

    val config = object : Config {
        override fun mitmConfig(): HttpInterceptorConfig {
            return object : HttpInterceptorConfig {
                override val httpInterceptorMatcher: HttpInterceptorMatcher
                    get() = object : HttpInterceptorMatcher {
                        override fun match(host: String): HttpInterceptor? {
                            return when {
                                host.contains("360") -> BaiduHttpInterceptor()
                                host.contains("baidu") -> BaiduHttpInterceptor()
                                else -> null
                            }
                        }
                    }

                override fun enableMitm(): Boolean = true

            }
        }

        val proxy = mutableMapOf<String, Proxy>("socks" to Socks5Proxy())

        override fun proxyMatcher(host: String): String {
            return when {
                host.contains("fengguang") -> "DIRECT"
                host.contains("baidu") -> "DIRECT"
                host.contains("google") -> "socks"
                else -> "DIRECT"
            }
        }

        override fun proxyList(): MutableMap<String, Proxy> {
            return proxy
        }
    }

    fun start(port: Int, remoteChannelClazz: Class<out Channel>) {

        val myModule: Module = applicationContext {
            bean("proxyConfig") { ProxyConfig(config) } // get() will resolve Repository instance
            bean("remoteChannelClazz") { remoteChannelClazz }
        }

        startKoin(listOf(myModule))

        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(SocksServerInitializer(UpstreamMatchHandler()))
            logger.debug("start server at: $port")
            b.bind(port).sync().channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
            closeKoin()
        }
    }
}