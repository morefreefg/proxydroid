package me.bwelco.proxy.proxy

import io.netty.channel.Channel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.upstream.Socks5Upstream
import me.bwelco.proxy.upstream.Upstream
import java.net.Inet4Address

class Socks5Proxy : Proxy() {

    override fun createProxyHandler(request: Socks5CommandRequest, promise: Promise<Channel>): Upstream {
        return Socks5Upstream(request = request,
                promise = promise,
                remoteSocks5Server = Inet4Address.getByName("192.168.2.105"),
                remoteSocks5ServerPort = 6153)
    }

}