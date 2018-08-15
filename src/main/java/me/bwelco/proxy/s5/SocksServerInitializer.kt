package me.bwelco.proxy.s5

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import java.net.Socket

class SocksServerInitializer(val socksServerConnectHandler: SocksServerConnectHandler): ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast(
                LoggingHandler(LogLevel.INFO),
                SocksPortUnificationServerHandler(),
                SocksServerHandler.newInstance(socksServerConnectHandler))
    }
}