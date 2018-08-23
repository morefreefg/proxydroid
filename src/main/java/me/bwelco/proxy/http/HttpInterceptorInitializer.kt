package me.bwelco.proxy.http

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.logging.LoggingHandler
import me.bwelco.proxy.upstream.RelayHandler

class HttpInterceptorInitializer(val remoteChannel: Channel,
                                 val httpInterceptor: HttpInterceptor): ChannelInitializer<SocketChannel>() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {

        super.channelRead(ctx, msg)
    }

    override fun initChannel(clientChannel: SocketChannel) {
        clientChannel.pipeline().addLast(HttpResponseEncoder())
        clientChannel.pipeline().addLast(HttpRequestDecoder())
        clientChannel.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))
        clientChannel.pipeline().addLast("HttpInterceptorHandler",
                HttpInterceptorHandler(httpInterceptor))
        clientChannel.pipeline().addLast(RelayHandler(remoteChannel))

        remoteChannel.pipeline().addLast(LoggingHandler())
        remoteChannel.pipeline().addLast(HttpResponseDecoder())
        remoteChannel.pipeline().addLast(HttpObjectAggregator(1024 * 1024 * 64))
        remoteChannel.pipeline().addLast(RelayHandler(clientChannel))
        remoteChannel.pipeline().addLast(HttpRequestEncoder())
    }

}