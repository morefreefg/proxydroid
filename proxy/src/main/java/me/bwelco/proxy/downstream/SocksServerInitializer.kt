package me.bwelco.proxy.downstream

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import me.bwelco.proxy.proxy.UpstreamMatchHandler

class SocksServerInitializer(val upstreamMatchHandler: UpstreamMatchHandler) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast(
                LoggingHandler(LogLevel.INFO),
                SocksPortUnificationServerHandler(),
                SocksServerHandler.newInstance(upstreamMatchHandler))
    }
}