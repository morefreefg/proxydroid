package me.bwelco.new.init

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel

class SocksProxyInitHandler : ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast()
    }

}