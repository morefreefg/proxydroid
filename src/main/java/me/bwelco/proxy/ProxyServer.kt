package me.bwelco.proxy

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import me.bwelco.proxy.http.HttpInterceptor
import me.bwelco.proxy.http.HttpInterceptorMatcher
import me.bwelco.proxy.s5.SocksServerConnectHandler
import me.bwelco.proxy.s5.SocksServerInitializer
import me.bwelco.proxy.tls.SSLFactory
import org.apache.commons.logging.LogFactory
import java.net.Socket
import java.nio.charset.Charset

fun main(args: Array<String>) {
    ProxyServer().start(1080)
}

class LoggingHttpInterceptor : HttpInterceptor {

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

class ProxyServer {

    val logger = LogFactory.getLog(ProxyServer::class.java)

    val socksServerConnectHandler = SocksServerConnectHandler({}, object : HttpInterceptorMatcher {
        override fun match(host: String): HttpInterceptor? {
            return LoggingHttpInterceptor()
        }
    })

    fun start(port: Int, onConnectListener: (Socket) -> Unit = {}) {
        SSLFactory.preloadCertificate(listOf("baidu.com"))

        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(SocksServerInitializer(socksServerConnectHandler))
            logger.debug("start server at: $port")
            b.bind(port).sync().channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}