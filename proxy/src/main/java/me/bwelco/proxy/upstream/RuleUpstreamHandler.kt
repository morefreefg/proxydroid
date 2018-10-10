package me.bwelco.proxy.upstream

import io.netty.channel.Channel
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise

class RuleUpstreamHandler(val request: Socks5CommandRequest,
                          val promise: Promise<Channel>) : UpstreamHandler(request, promise) {

}