package me.bwelco.proxy.upstream

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise


class RejectUpstreamHandler(val upstreamHandler: UpstreamHandlerParam) : UpstreamHandler(upstreamHandler) {

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.channel().pipeline().remove(this)
        upstreamHandler.remoteChannelPromise.setFailure(Throwable("request: ${upstreamHandler.socks5Request} rejected"))
    }
}