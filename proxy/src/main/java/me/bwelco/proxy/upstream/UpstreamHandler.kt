package me.bwelco.proxy.upstream

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest
import io.netty.util.concurrent.Promise
import org.koin.standalone.KoinComponent
import java.rmi.Remote

/**
 * request: upstream socks request
 * promise: when set promise success, will start to direct downstream to upstream
 */

class UpstreamHandlerParam(val socks5Request: Socks5CommandRequest,
                           val remoteChannelPromise: Promise<Channel>)

abstract class UpstreamHandler(upstreamHandler: UpstreamHandlerParam) : ChannelInboundHandlerAdapter(), KoinComponent