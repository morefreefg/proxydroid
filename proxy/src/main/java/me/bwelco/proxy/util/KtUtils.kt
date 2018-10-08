package me.bwelco.proxy.util

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import java.nio.charset.Charset

fun ChannelFuture.addFutureListener(result: (ChannelFuture) -> Unit) {
    this.addListener(object : ChannelFutureListener {
        override fun operationComplete(future: ChannelFuture) {
            result(future)
        }
    })
}

fun String?.isEmpty(): Boolean {
    return (this == null || this.length == 0)
}

fun ByteBuf.string(): String = this.toString(Charset.forName("UTF-8"))

fun Channel.closeOnFlush() {
    if (isActive) {
        writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
}