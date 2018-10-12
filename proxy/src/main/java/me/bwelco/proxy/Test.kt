package me.bwelco.proxy

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import me.bwelco.proxy.util.addFutureListener

fun main(args: Array<String>) {
        val bootstrap = Bootstrap()

    bootstrap.group(NioEventLoopGroup())
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(object : ChannelInboundHandlerAdapter() {
                override fun channelActive(ctx: ChannelHandlerContext?) {
                    println("channel active")
                    super.channelActive(ctx)
                }
            })

    bootstrap.connect("ladder.fengguang.me", 443).addFutureListener {
        if (it.isSuccess) {
            println("channel active 1")

            val i = 0
        } else {
            println("channel active 2")
            val i = 1
        }
    }

    while (true) {
    }
}