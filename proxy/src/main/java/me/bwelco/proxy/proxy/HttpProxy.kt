package me.bwelco.proxy.proxy

import io.netty.channel.Channel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.upstream.HttpUpstream
import me.bwelco.proxy.upstream.Upstream
import java.net.InetAddress


class HttpProxy(val address: InetAddress,
                val port: Int) : Proxy() {

    override fun createProxyHandler(request: Socks5CommandRequest, promise: Promise<Channel>): Upstream {
        return HttpUpstream(request, promise, address, port)
    }

}