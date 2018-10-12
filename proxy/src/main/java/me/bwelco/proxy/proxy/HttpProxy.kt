package me.bwelco.proxy.proxy

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.action.DirectAction
import me.bwelco.proxy.action.FollowUpAction
import me.bwelco.proxy.upstream.HttpUpstreamHandler
import me.bwelco.proxy.upstream.UpstreamHandler
import me.bwelco.proxy.upstream.UpstreamHandlerParam
import java.net.InetAddress


class HttpProxy(val address: InetAddress,
                val port: Int) : Proxy() {
    override fun createProxyHandler(upstreamHandlerParam: UpstreamHandlerParam): UpstreamHandler {
        return HttpUpstreamHandler(upstreamHandlerParam, address, port)
    }

    override fun followUpAction(): FollowUpAction = DirectAction()
}