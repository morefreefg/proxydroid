package me.bwelco.new.client

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

abstract class ProxyClient {

    private var isStarted: Boolean = false
    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null

    fun startUp() {
        if (isStarted) { return }
        isStarted = true

        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()

        val serverBootstrap = ServerBootstrap()

        try {
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childHandler(initHandler)
                    .childOption(ChannelOption.SO_REUSEADDR, true)

            serverBootstrap.bind(port).sync().channel().closeFuture().sync()
        } finally {
            shutDown()
        }
    }

    fun shutDown() {
        if (!isStarted) { return }
        isStarted = false
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
    }

    abstract val port: Int
    abstract val initHandler: ChannelHandler
}