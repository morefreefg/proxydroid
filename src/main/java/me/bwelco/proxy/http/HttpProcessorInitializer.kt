package me.bwelco.proxy.http

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import me.bwelco.proxy.upstream.RelayHandler

class HttpProcessorInitializer(val clientChannel: Channel,
                               val connectFuture: ChannelFuture) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(serverChannel: SocketChannel) {
        connectFuture.addListener { future ->
            if (future.isSuccess) {
                clientChannel.pipeline().addLast(RelayHandler(serverChannel))
                serverChannel.pipeline().addLast(RelayHandler(clientChannel))
            }
        }
    }

}