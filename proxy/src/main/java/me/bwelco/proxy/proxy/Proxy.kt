package me.bwelco.proxy.proxy

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.action.FollowUpAction
import me.bwelco.proxy.upstream.UpstreamHandler
import me.bwelco.proxy.upstream.UpstreamHandlerParam
import java.rmi.Remote

abstract class Proxy {
    abstract fun createProxyHandler(upstreamHandlerParam: UpstreamHandlerParam): UpstreamHandler

    abstract fun followUpAction(): FollowUpAction
}