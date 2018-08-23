package me.bwelco.proxy.proxy

import io.netty.channel.Channel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.upstream.RejectUpstream
import me.bwelco.proxy.upstream.Upstream

class RejectProxy : Proxy() {

    override fun createProxyHandler(request: Socks5CommandRequest, promise: Promise<Channel>): Upstream {
        return RejectUpstream(request, promise)
    }

}