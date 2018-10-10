package me.bwelco.proxy.proxy

import io.netty.channel.Channel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.action.FollowUpAction
import me.bwelco.proxy.upstream.UpstreamHandler

abstract class Proxy {
    abstract fun createProxyHandler(request: Socks5CommandRequest,
                                    promise: Promise<Channel>): UpstreamHandler

    abstract fun followUpAction(): FollowUpAction
}