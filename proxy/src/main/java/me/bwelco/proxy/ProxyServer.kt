package me.bwelco.proxy

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import me.bwelco.proxy.downstream.SocksServerInitializer
import me.bwelco.proxy.proxy.UpstreamMatchHandler
import me.bwelco.proxy.rule.CustomRules
import me.bwelco.proxy.rule.DefaultRules
import me.bwelco.proxy.rule.ProxyRules
import me.bwelco.proxy.rule.Rules
import org.koin.dsl.module.Module
import org.koin.dsl.module.applicationContext
import org.koin.error.AlreadyStartedException
import org.koin.standalone.StandAloneContext.closeKoin
import org.koin.standalone.StandAloneContext.startKoin

fun main(args: Array<String>) {
    ProxyServer.startUp(rules = CustomRules())
}

object ProxyServer {

    private var isStarted = false
    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null

    fun startUp(port: Int = 1080,
              remoteChannelClazz: Class<out Channel> = NioSocketChannel::class.java,
              rules: Rules = DefaultRules()) {

        if (isStarted) return

        val myModule: Module = applicationContext {
            bean("proxyConfig") { ProxyRules(rules) } // get() will resolve Repository instance
            bean("remoteChannelClazz") { remoteChannelClazz }
        }

        try { startKoin(listOf(myModule)) } catch (e: AlreadyStartedException) { }

        isStarted = true

        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()

        try {
            val serverBootstrap = ServerBootstrap()

            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childHandler(SocksServerInitializer(UpstreamMatchHandler()))
                    .childOption(ChannelOption.SO_REUSEADDR, true)

            serverBootstrap.bind(port).sync().channel().closeFuture().sync()
        } finally {
            bossGroup?.shutdownGracefully()
            workerGroup?.shutdownGracefully()
            closeKoin()
            isStarted = false
        }
    }

    fun shutdown() {
        bossGroup?.shutdownGracefully()
        workerGroup?.shutdownGracefully()
        closeKoin()
        isStarted = false
    }

}