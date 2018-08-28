package me.bwelco.proxy.upstream

import io.netty.channel.Channel
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise

/**
 * request: upstream socks request
 * promise: when set promise success, will start to direct downstream to upstream
 */
abstract class Upstream(request: Socks5CommandRequest,
                        promise: Promise<Channel>) : ChannelInboundHandlerAdapter()