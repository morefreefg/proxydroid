package me.bwelco.proxy.util

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener

fun ChannelFuture.addFutureListener(result: (ChannelFuture) -> Unit) {
    this.addListener(object : ChannelFutureListener {
        override fun operationComplete(future: ChannelFuture) {
            result(future)
        }
    })
}