package me.bwelco.proxy.util

import io.netty.buffer.ByteBuf
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


fun ByteBuf.string(): String = this.toString(Charset.forName("UTF-8" ))