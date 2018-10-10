package me.bwelco.proxy.upstream

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise


class RejectUpstreamHandler(val request: Socks5CommandRequest,
                            val promise: Promise<Channel>) : UpstreamHandler(request, promise) {

    override fun channelActive(ctx: ChannelHandlerContext?) {
        promise.setFailure(Throwable("request: $request rejected"))
    }
}