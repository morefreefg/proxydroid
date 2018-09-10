package me.bwelco.proxy

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import me.bwelco.proxy.rule.Rules
import me.bwelco.proxy.rule.ProxyRules
import me.bwelco.proxy.downstream.SocksServerInitializer
import me.bwelco.proxy.http.HttpInterceptor
import me.bwelco.proxy.proxy.UpstreamMatchHandler
import me.bwelco.proxy.rule.DefaultRules
import org.koin.dsl.module.Module
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext.closeKoin
import org.koin.standalone.StandAloneContext.startKoin

fun main(args: Array<String>) {
    ProxyServer.start()
}

object ProxyServer {

    fun start(port: Int = 1080,
              remoteChannelClazz: Class<out Channel> = NioSocketChannel::class.java,
              rules: Rules = DefaultRules()) {

        val myModule: Module = applicationContext {
            bean("proxyConfig") { ProxyRules(rules) } // get() will resolve Repository instance
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
            b.bind(port).sync().channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
            closeKoin()
        }
    }
}