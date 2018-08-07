package me.bwelco.socks

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import me.bwelco.socks.init.Socks5ServerInitializer
import me.bwelco.socks.inner.SocksServerInitializer
import java.lang.Exception
import java.net.Socket

fun main(args: Array<String>) {

}

class ProxyServer {

    fun startBuildidSocksServer(port: Int, onConnectListener: (Socket) -> Unit = {}) {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .handler(LoggingHandler(LogLevel.INFO))
                    .childHandler(SocksServerInitializer(onConnectListener))
            b.bind(port).sync().channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }


    fun start(port: Int, onConnect: (Socket) -> Unit = {}) {
        val boss = NioEventLoopGroup()
        val worker = NioEventLoopGroup()

        try {
            val bootstrap = ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel::class.java)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(Socks5ServerInitializer(boss, onConnect))

            val future = bootstrap.bind(port).sync()
            future.channel().closeFuture().sync()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}