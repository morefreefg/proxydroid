package me.bwelco.proxy

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import me.bwelco.proxy.s5.SocksServerInitializer
import org.apache.commons.logging.LogFactory
import java.net.Proxy
import java.net.Socket

fun main(args: Array<String>) {
    ProxyServer().start(1080)
}

class ProxyServer {

    val logger = LogFactory.getLog(ProxyServer::class.java)

    fun start(port: Int, onConnectListener: (Socket) -> Unit = {}) {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .handler(LoggingHandler(LogLevel.INFO))
                    .childHandler(SocksServerInitializer(onConnectListener))
            logger.debug("start server at: $port")
            b.bind(port).sync().channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}