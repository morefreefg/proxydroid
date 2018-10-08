package me.bwelco.proxy.proxy

import io.netty.channel.Channel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.upstream.Socks5Upstream
import me.bwelco.proxy.upstream.Upstream
import java.net.InetAddress

class Socks5Proxy(val address: InetAddress,
                  val port: Int) : Proxy() {

    override fun createProxyHandler(request: Socks5CommandRequest, promise: Promise<Channel>): Upstream {
        return Socks5Upstream(request = request,
                promise = promise,
                remoteSocks5Server = address,
                remoteSocks5ServerPort = port)
    }

}