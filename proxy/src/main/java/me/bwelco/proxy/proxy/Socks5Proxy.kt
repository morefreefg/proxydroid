package me.bwelco.proxy.proxy

import io.netty.channel.Channel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.action.DirectAction
import me.bwelco.proxy.action.FollowUpAction
import me.bwelco.proxy.upstream.Socks5UpstreamHandler
import me.bwelco.proxy.upstream.UpstreamHandler
import java.net.InetAddress

class Socks5Proxy(val address: InetAddress,
                  val port: Int) : Proxy() {

    override fun followUpAction(): FollowUpAction = DirectAction()

    override fun createProxyHandler(request: Socks5CommandRequest, promise: Promise<Channel>): UpstreamHandler {
        return Socks5UpstreamHandler(request = request,
                promise = promise,
                remoteSocks5Server = address,
                remoteSocks5ServerPort = port)
    }

}