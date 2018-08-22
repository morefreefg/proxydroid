package me.bwelco.proxy.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ssl.AbstractSniHandler
import io.netty.handler.ssl.SslContext
import io.netty.util.concurrent.Future
import me.bwelco.proxy.util.isEmpty

class MySniHandler : AbstractSniHandler<SslContext>() {


    override fun onLookupComplete(ctx: ChannelHandlerContext, hostname: String?, future: Future<SslContext>) {
        if (hostname.isEmpty()) return

    }

    override fun lookup(ctx: ChannelHandlerContext, hostname: String?): Future<SslContext> {
        return ctx.executor().newPromise<SslContext>().apply {
            this.setSuccess(null)
        }
    }

}