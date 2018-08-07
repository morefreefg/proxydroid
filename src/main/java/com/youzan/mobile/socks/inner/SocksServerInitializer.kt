package com.youzan.mobile.socks.inner

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import java.net.Socket

class SocksServerInitializer(val connectListener: (Socket) -> Unit): ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel?) {
        if (ch == null) return

        ch.pipeline().addLast(
                LoggingHandler(LogLevel.DEBUG),
                SocksPortUnificationServerHandler(),
                SocksServerHandler.newInstance(connectListener))
    }

}