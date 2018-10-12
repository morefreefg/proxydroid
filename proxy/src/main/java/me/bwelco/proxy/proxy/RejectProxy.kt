package me.bwelco.proxy.proxy

import io.netty.channel.Channel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import me.bwelco.proxy.action.DirectAction
import me.bwelco.proxy.action.FollowUpAction
import me.bwelco.proxy.upstream.RejectUpstreamHandler
import me.bwelco.proxy.upstream.UpstreamHandler
import me.bwelco.proxy.upstream.UpstreamHandlerParam

class RejectProxy : Proxy() {
    override fun createProxyHandler(upstreamHandlerParam: UpstreamHandlerParam): UpstreamHandler {
        return RejectUpstreamHandler(upstreamHandlerParam)
    }

    override fun followUpAction(): FollowUpAction = DirectAction()

}